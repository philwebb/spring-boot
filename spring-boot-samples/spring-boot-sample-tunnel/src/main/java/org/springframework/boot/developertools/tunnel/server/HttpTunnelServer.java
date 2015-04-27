/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.developertools.tunnel.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * A server that can be used to tunnel TCP traffic over HTTP. Similar in design to the <a
 * href="http://xmpp.org/extensions/xep-0124.html">Bidirectional-streams Over Synchronous
 * HTTP (BOSH)</a> XMPP extension protocol, the server uses long polling with HTTP
 * requests held open until a response is available. A typical traffic pattern would be as
 * follows:
 *
 * <pre>
 * [ CLIENT ]                      [ SERVER ]
 *     | (a) Initial empty request     |
 *     |------------------------------}|
 *     | (b) Data I                    |
 *  --}|------------------------------}|---}
 *     | Response I (a)                |
 *  {--|<------------------------------|{---
 *     |                               |
 *     | (c) Data II                   |
 *  --}|------------------------------}|---}
 *     | Response II (b)               |
 *  {--|{------------------------------|{---
 *     .                               .
 *     .                               .
 * </pre>
 *
 * Each incoming request is held open to be used to carry the next available response. The
 * server will hold at most two connections open at any given time.
 * <p>
 * Requests should be made using HTTP POST, with any payload contained in the body. The
 * following response codes can be returned from the server:
 * <p>
 * <table>
 * <tr>
 * <th>Status</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>200 (OK)</td>
 * <td>Data payload response.</td>
 * </tr>
 * <tr>
 * <td>204 (No Content)</td>
 * <td>The long poll has timed out and the client should start a new request.</td>
 * </tr>
 * <tr>
 * <td>410 (Gone)</td>
 * <td>The target server has disconnected.</td>
 * </tr>
 * </table>
 * <p>
 * Requests and responses that contain payloads include a {@code x-seq} header that
 * contains a running sequence number (used to ensure data is applied in the correct
 * order). The first request containing a payload should have a {@code x-seq} value of
 * {@code 1}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see org.springframework.boot.developertools.tunnel.client.HttpTunnelConnection
 */
public class HttpTunnelServer {

	private static final int SECONDS = 1000;

	private static final int DEFAULT_LONG_POLL_TIMEOUT = 10 * SECONDS;

	private static final long DEFAULT_DISCONNECT_TIMEOUT = 30 * SECONDS;

	private static final int BUFFER_SIZE = 1024 * 10;

	private static final MediaType DISCONNECT_MEDIA_TYPE = new MediaType("application",
			"x-disconnect");

	private static final String SEQ_HEADER = "x-seq";

	private static final Log logger = LogFactory.getLog(HttpTunnelServer.class);

	private final TargetServerConnection serverConnection;

	private int longPollTimeout = DEFAULT_LONG_POLL_TIMEOUT;

	private long disconnectTimeout = DEFAULT_DISCONNECT_TIMEOUT;

	private volatile ServerThread serverThread;

	/**
	 * Creates a new {@link HttpTunnelServer} instance.
	 * @param serverConnection the connection to the target server
	 */
	public HttpTunnelServer(TargetServerConnection serverConnection) {
		Assert.notNull(serverConnection, "ServerConnection must not be null");
		this.serverConnection = serverConnection;
	}

	/**
	 * Handle an incoming HTTP connection.
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws IOException
	 */
	public void handle(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException {
		handle(new HttpConnection(request, response));
	}

	/**
	 * Handle an incoming HTTP connection.
	 * @param httpConnection the HTTP connection
	 * @throws IOException
	 */
	protected void handle(HttpConnection httpConnection) throws IOException {
		getServerThread().handleIncomingHttp(httpConnection);
		httpConnection.waitForResponse();
	}

	/**
	 * Returns the active server thread, creating and starting it if necessary.
	 * @return the {@code ServerThread} (never {@code null})
	 * @throws IOException
	 */
	protected ServerThread getServerThread() throws IOException {
		synchronized (this) {
			if (this.serverThread == null) {
				ByteChannel channel = this.serverConnection.open(this.longPollTimeout);
				this.serverThread = new ServerThread(channel);
				this.serverThread.start();
			}
			return this.serverThread;
		}
	}

	/**
	 * Called when the server thread exits.
	 */
	void clearServerThread() {
		synchronized (this) {
			this.serverThread = null;
		}
	}

	/**
	 * Set the long poll timeout for the server.
	 * @param longPollTimeout the long poll timeout in milliseconds
	 */
	public void setLongPollTimeout(int longPollTimeout) {
		Assert.isTrue(longPollTimeout > 0, "LongPollTimeout must be a positive value");
		this.longPollTimeout = longPollTimeout;
	}

	/**
	 * Set the maximum amount of time to wait for a client before closing the connection.
	 * @param disconnectTimeout the disconnect timeout in milliseconds
	 */
	public void setDisconnectTimeout(long disconnectTimeout) {
		Assert.isTrue(disconnectTimeout > 0, "DisconnectTimeout must be a positive value");
		this.disconnectTimeout = disconnectTimeout;
	}

	/**
	 * The main server thread used to transfer tunnel traffic.
	 */
	protected class ServerThread extends Thread {

		private final ByteChannel targetServer;

		private final Deque<HttpConnection> httpConnections;

		private boolean closed;

		private AtomicLong responseSeq = new AtomicLong();

		private long lastRequestSeq = 0;

		private Map<Long, ByteBuffer> pendingForwards = new HashMap<Long, ByteBuffer>();

		public ServerThread(ByteChannel targetServer) {
			Assert.notNull(targetServer, "TargetServer must not be null");
			this.targetServer = targetServer;
			this.httpConnections = new ArrayDeque<HttpConnection>(2);
		}

		@Override
		public void run() {
			try {
				try {
					readAndForwardTargetServerData();
				}
				catch (Exception ex) {
					logger.trace("Unexpected exception from tunnel server", ex);
				}
			}
			finally {
				this.closed = true;
				closeHttpConnections();
				closeTargetServer();
				HttpTunnelServer.this.clearServerThread();
			}
		}

		private void readAndForwardTargetServerData() throws IOException {
			while (this.targetServer.isOpen()) {
				ByteBuffer payload = getTargetServerData();
				synchronized (this.httpConnections) {
					if (payload != null) {
						HttpConnection connection = getOrWaitForHttpConnection(DequeOperation.POLL_FIRST);
						connection.respond(payload, this.responseSeq.incrementAndGet());
					}
					closeStaleHttpConnections();
					getOrWaitForHttpConnection(DequeOperation.PEEK_FIRST);
				}
			}
		}

		private ByteBuffer getTargetServerData() throws IOException {
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			try {
				int amountRead = this.targetServer.read(buffer);
				Assert.state(amountRead != -1, "Target server connection closed");
				buffer.flip();
				return buffer;
			}
			catch (SocketTimeoutException ex) {
				return null;
			}
		}

		private HttpConnection getOrWaitForHttpConnection(DequeOperation operation) {
			synchronized (this.httpConnections) {
				HttpConnection httpConnection = operation.apply(this.httpConnections);
				if (httpConnection == null) {
					try {
						wait(HttpTunnelServer.this.disconnectTimeout);
					}
					catch (InterruptedException ex) {
					}
					httpConnection = operation.apply(this.httpConnections);
					Assert.state(httpConnection != null, "Timeout waiting for HTTP");
				}
				return httpConnection;
			}
		}

		private void closeStaleHttpConnections() throws IOException {
			synchronized (this.httpConnections) {
				Iterator<HttpConnection> iterator = this.httpConnections.iterator();
				while (iterator.hasNext()) {
					HttpConnection httpConnection = iterator.next();
					if (httpConnection.isOlderThan(HttpTunnelServer.this.longPollTimeout)) {
						httpConnection.respond(HttpStatus.NO_CONTENT);
						iterator.remove();
					}
				}
			}
		}

		private void closeHttpConnections() {
			synchronized (this.httpConnections) {
				while (!this.httpConnections.isEmpty()) {
					try {
						this.httpConnections.removeFirst().respond(HttpStatus.GONE);
					}
					catch (Exception ex) {
						logger.trace("Unable to close remote HTTP connection");
					}
				}
			}
		}

		private void closeTargetServer() {
			try {
				this.targetServer.close();
			}
			catch (IOException ex) {
				logger.trace("Unable to target server connection");
			}
		}

		/**
		 * Handle an incoming {@link HttpConnection}.
		 * @param httpConnection the connection to handle.
		 * @throws IOException
		 */
		public void handleIncomingHttp(HttpConnection httpConnection) throws IOException {
			if (this.closed) {
				httpConnection.respond(HttpStatus.GONE);
			}
			synchronized (this.httpConnections) {
				while (this.httpConnections.size() > 1) {
					this.httpConnections.removeFirst().respond(HttpStatus.NO_CONTENT);
				}
				this.httpConnections.addLast(httpConnection);
			}
			forwardToTargetServer(httpConnection);
		}

		private void forwardToTargetServer(HttpConnection httpConnection)
				throws IOException {
			if (httpConnection.isDisconnectRequest()) {
				this.targetServer.close();
				interrupt();
			}
			ByteBuffer payload = httpConnection.getPayload();
			if (payload != null) {
				synchronized (this.targetServer) {
					forwardToTargetServer(httpConnection.getRequestSeq(), payload);
				}
			}
		}

		private void forwardToTargetServer(Long seq, ByteBuffer payload)
				throws IOException {
			Assert.notNull(seq, "Missing " + SEQ_HEADER + " header from request");
			if (this.lastRequestSeq != seq - 1) {
				Assert.state(this.pendingForwards.size() < 10,
						"Too many requests pending");
				this.pendingForwards.put(seq, payload);
				return;
			}
			while (payload.hasRemaining()) {
				this.targetServer.write(payload);
			}
			this.lastRequestSeq = seq;
			ByteBuffer pendingForward = this.pendingForwards.get(seq + 1);
			if (pendingForward != null) {
				forwardToTargetServer(seq + 1, pendingForward);
			}
		}

	}

	/**
	 * Operations that can be performed on a {@link Deque}.
	 */
	protected static enum DequeOperation {
		POLL_FIRST {
			@Override
			public <E> E apply(Deque<E> deque) {
				return deque.pollFirst();
			}
		},
		PEEK_FIRST {
			@Override
			public <E> E apply(Deque<E> deque) {
				return deque.peekFirst();
			}
		};

		public abstract <E> E apply(Deque<E> deque);
	}

	/**
	 * Encapsulates a HTTP request/response pair.
	 */
	protected static class HttpConnection {

		private final long createTime;

		private final ServerHttpRequest request;

		private final ServerHttpResponse response;

		private ServerHttpAsyncRequestControl async;

		private volatile boolean complete = false;

		public HttpConnection(ServerHttpRequest request, ServerHttpResponse response) {
			this.createTime = System.currentTimeMillis();
			this.request = request;
			this.response = response;
			this.async = startAsync();
		}

		/**
		 * Start asynchronous support or if unavailble return {@code null} to cause
		 * {@link #waitForResponse()} to block.
		 * @return the async request control
		 */
		protected ServerHttpAsyncRequestControl startAsync() {
			try {
				// Try to use async to save blocking
				ServerHttpAsyncRequestControl async = this.request
						.getAsyncRequestControl(this.response);
				async.start();
				return async;
			}
			catch (Exception ex) {
				return null;
			}
		}

		/**
		 * Return the underlying request (used for testing)
		 * @return the request
		 */
		protected final ServerHttpRequest getRequest() {
			return this.request;
		}

		/**
		 * Return the underlying response (used for testing)
		 * @return the response
		 */
		protected final ServerHttpResponse getResponse() {
			return this.response;
		}

		/**
		 * Determine if a connection is older than the specified time.
		 * @param time the time to check
		 * @return {@code true} if the request is older than the time
		 */
		public boolean isOlderThan(int time) {
			long runningTime = System.currentTimeMillis() - this.createTime;
			return (runningTime > time);
		}

		/**
		 * Cause the request to block or use asynchronous methods to wait until a response
		 * is available.
		 */
		public void waitForResponse() {
			if (this.async == null) {
				while (!this.complete) {
					try {
						synchronized (this) {
							wait(1000);
						}
					}
					catch (InterruptedException ex) {
					}
				}
			}
		}

		/**
		 * Detect if the request is actually a signal to disconnect.
		 * @return if the request is a signal to disconnect
		 */
		public boolean isDisconnectRequest() {
			return DISCONNECT_MEDIA_TYPE.equals(this.request.getHeaders()
					.getContentType());
		}

		public Long getRequestSeq() {
			String headerValue = this.request.getHeaders().getFirst(SEQ_HEADER);
			return (headerValue == null ? null : Long.valueOf(headerValue));
		}

		/**
		 * @return the payload from the HTTP request.
		 * @throws IOException
		 */
		public ByteBuffer getPayload() throws IOException {
			long length = this.request.getHeaders().getContentLength();
			if (length <= 0) {
				return null;
			}
			ReadableByteChannel body = Channels.newChannel(this.request.getBody());
			ByteBuffer payload = ByteBuffer.allocate((int) length);
			while (payload.hasRemaining()) {
				body.read(payload);
			}
			payload.flip();
			return payload;
		}

		/**
		 * Send a HTTP status response.
		 * @param status the status to send
		 * @throws IOException
		 */
		public void respond(HttpStatus status) throws IOException {
			Assert.notNull(status, "Status must not be null");
			respond(status, null, -1);
		}

		/**
		 * Set a payload response
		 * @param payload the payload to send
		 * @param seq the sequence number of the response
		 * @throws IOException
		 */
		public void respond(ByteBuffer payload, long seq) throws IOException {
			Assert.notNull(payload, "Payload must not be null");
			respond(HttpStatus.OK, payload, seq);
		}

		private void respond(HttpStatus status, ByteBuffer payload, long seq)
				throws IOException {
			this.response.setStatusCode(status);
			if (payload != null) {
				this.response.getHeaders().setContentLength(payload.remaining());
				this.response.getHeaders().add(SEQ_HEADER, Long.toString(seq));
				WritableByteChannel body = Channels.newChannel(this.response.getBody());
				while (payload.hasRemaining()) {
					body.write(payload);
				}
				body.close();
			}
			complete();
		}

		/**
		 * Called when a request is complete.
		 */
		protected void complete() {
			if (this.async != null) {
				this.async.complete();
			}
			else {
				synchronized (this) {
					this.complete = true;
					notifyAll();
				}
			}
		}

	}

}
