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

package org.springframework.boot.livereload.tunnel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 */
public class TunnelServerHttpHandler {

	private static final int BUFFER_SIZE = 1024 * 2;

	private static final int DEFAULT_LONG_POLL_TIMEOUT = 1000;

	private static final int DEFAULT_DISCONNECT_TIMEOUT = 1000 * 10;

	private int connectionPort;

	private int longPollTimeout = DEFAULT_LONG_POLL_TIMEOUT;

	private int diconnectTimeout = DEFAULT_DISCONNECT_TIMEOUT;

	private HandlerThread handlerThread;

	public TunnelServerHttpHandler(int connectionPort) {
		this.connectionPort = connectionPort;
	}

	public void setLongPollTimeout(int longPollTimeout) {
		this.longPollTimeout = longPollTimeout;
	}

	public void setDiconnectTimeout(int diconnectTimeout) {
		this.diconnectTimeout = diconnectTimeout;
	}

	public void handleHttp(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		synchronized (this) {
			if (this.handlerThread == null) {
				this.handlerThread = new HandlerThread();
				this.handlerThread.start();
			}
		}
		this.handlerThread.add(new WebConnection(request.startAsync()));
	}

	private class HandlerThread extends Thread {

		private final SocketChannel socketChannel;

		private final WebConnections webConnections = new WebConnections();

		public HandlerThread() throws IOException {
			setName("Tunnel Connection");
			setDaemon(true);
			SocketAddress address = new InetSocketAddress(
					TunnelServerHttpHandler.this.connectionPort);
			this.socketChannel = SocketChannel.open(address);
			this.socketChannel.socket().setSoTimeout(
					TunnelServerHttpHandler.this.longPollTimeout);
		}

		public void add(WebConnection webConnection) throws IOException {
			ByteBuffer payload = webConnection.getPayload();
			// System.out.println("Adding connection "
			// + (payload == null ? "no" : payload.remaining()) + " in payload");
			if (payload != null) {
				this.socketChannel.write(payload);
			}
			this.webConnections.add(webConnection);
		}

		@Override
		public void run() {
			try {
				try {
					readLoop();
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			finally {
				try {
					this.socketChannel.close();
					// We need a way to send this back to the client
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
				synchronized (TunnelServerHttpHandler.this) {
					TunnelServerHttpHandler.this.handlerThread = null;
				}
			}
		}

		private void readLoop() throws IOException {
			while (true) {
				ByteBuffer payload = readFromSocket();
				if (payload != null) {
					this.webConnections.respond(payload);
				}
				this.webConnections.cleanupStaleConnections();
			}
		}

		private ByteBuffer readFromSocket() throws IOException {
			try {
				ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				// System.out.println("Blocked on read");
				ReadableByteChannel channel = Channels.newChannel(this.socketChannel
						.socket().getInputStream());
				int amountRead = channel.read(buffer);
				// int amountRead = this.socketChannel.read(buffer);
				// System.out.println("Read " + amountRead + " from socket");
				Assert.state(amountRead != -1);
				buffer.flip();
				return (amountRead > 0 ? buffer : null);
			}
			catch (SocketTimeoutException ex) {
				// System.out.println("Got a socket read timeout");
				return null;
			}
		}

		private void close() {
			if (this.socketChannel != null) {
				try {
					this.socketChannel.close();
					// We need a way to send this back to the client
				}
				catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}

	}

	private class WebConnections {

		private Deque<WebConnection> connections = new LinkedList<WebConnection>();

		public void add(WebConnection webConnection) {
			synchronized (this) {
				this.connections.add(webConnection);
				notify();
			}
		}

		public void respond(ByteBuffer buffer) throws IOException {
			WebConnection connection = pollForConnection();
			// System.out.println("Responding " + this.connections.size());
			connection.respondWithPayload(buffer);
		}

		private WebConnection pollForConnection() {
			synchronized (this) {
				try {
					return this.connections.removeFirst();
				}
				catch (NoSuchElementException ex) {
					cleanupStaleConnections();
					return pollForConnection();
				}
			}
		}

		public void cleanupStaleConnections() {
			synchronized (this) {
				// System.out.println("Cleaning stale " + this.connections.size());
				Iterator<WebConnection> iterator = this.connections.iterator();
				while (iterator.hasNext()) {
					WebConnection connection = iterator.next();
					if (connection.hasTimedOut()) {
						// System.out.println("Cleaning timeout");
						connection.respondWithNoData();
						iterator.remove();
					}
				}
				while (this.connections.size() > 3) {
					this.connections.removeFirst().respondWithNoData();
				}
				if (this.connections.isEmpty()) {
					try {
						// System.out.println("Waiting for fresh connection");
						wait(TunnelServerHttpHandler.this.diconnectTimeout);
					}
					catch (InterruptedException ex) {
						// System.out.println("Interrupted");
						ex.printStackTrace();
						// Either because an item was add or a timeout
					}
				}
				Assert.state(!this.connections.isEmpty(),
						"Timeout waiting for web connection");
			}
		}

	}

	private class WebConnection {

		private final AsyncContext context;

		private final long createTime;

		public WebConnection(AsyncContext context) {
			this.context = context;
			this.createTime = System.currentTimeMillis();
		}

		public ByteBuffer getPayload() throws IOException {
			HttpServletRequest request = (HttpServletRequest) this.context.getRequest();
			ReadableByteChannel channel = Channels.newChannel(request.getInputStream());
			int contentLength = request.getContentLength();
			if (contentLength <= 0) {
				return null;
			}
			ByteBuffer payload = ByteBuffer.allocate(contentLength);
			while (payload.hasRemaining()) {
				channel.read(payload);
			}
			payload.flip();
			return payload;
		}

		public void respondWithPayload(ByteBuffer buffer) throws IOException {
			HttpServletResponse response = (HttpServletResponse) this.context
					.getResponse();
			response.setContentLength(buffer.remaining());
			WritableByteChannel channel = Channels.newChannel(response.getOutputStream());
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
			channel.close();
			this.context.complete();
		}

		public void respondWithNoData() {
			((HttpServletResponse) this.context.getResponse()).setStatus(204);
			this.context.complete();
		}

		public boolean hasTimedOut() {
			int timeout = TunnelServerHttpHandler.this.longPollTimeout;
			return (System.currentTimeMillis() - this.createTime) > timeout;
		}

	}

}
