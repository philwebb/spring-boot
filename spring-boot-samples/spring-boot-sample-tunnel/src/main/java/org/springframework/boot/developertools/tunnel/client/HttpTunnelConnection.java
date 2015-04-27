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

package org.springframework.boot.developertools.tunnel.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link TunnelConnection} implementation that uses HTTP to transfer data.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see TunnelClient
 * @see org.springframework.boot.developertools.tunnel.server.HttpTunnelServer
 */
public class HttpTunnelConnection implements TunnelConnection {

	private static final String SEQ_HEADER = "x-seq";

	private static Log logger = LogFactory.getLog(HttpTunnelConnection.class);

	private final URI uri;

	private final ClientHttpRequestFactory requestFactory;

	public HttpTunnelConnection(String url) {
		this(url, new SimpleClientHttpRequestFactory());
	}

	protected HttpTunnelConnection(String url, ClientHttpRequestFactory requestFactory) {
		Assert.notNull(url, "URL must not be null");
		Assert.hasLength(url, "URL must not be empty");
		try {
			this.uri = new URL(url).toURI();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Malformed URL '" + url + "'");
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL '" + url + "'");
		}
		this.requestFactory = requestFactory;
	}

	@Override
	public WritableByteChannel open(WritableByteChannel incomingChannel,
			Closeable closeable) throws Exception {
		return new TunnelChannel(incomingChannel, closeable);
	}

	protected final ClientHttpRequest createRequest() throws IOException {
		return this.requestFactory.createRequest(this.uri, HttpMethod.POST);
	}

	/**
	 * A {@link WritableByteChannel} used to transfer traffic.
	 */
	private class TunnelChannel implements WritableByteChannel {

		private final WritableByteChannel incomingChannel;

		private final Closeable closeable;

		private boolean open = true;

		private AtomicLong requestSeq = new AtomicLong();

		private long lastRequestSeq = 0;

		private Map<Long, ByteBuffer> pendingForwards = new HashMap<Long, ByteBuffer>();

		private final ExecutorService executor = Executors
				.newCachedThreadPool(new TunnelThreadFactory());

		public TunnelChannel(WritableByteChannel incomingChannel, Closeable closeable) {
			this.incomingChannel = incomingChannel;
			this.closeable = closeable;
			openNewConnection(null, -1);
		}

		@Override
		public boolean isOpen() {
			return this.open;
		}

		@Override
		public void close() throws IOException {
			if (this.open) {
				this.open = false;
				this.closeable.close();
			}
		}

		@Override
		public synchronized int write(ByteBuffer src) throws IOException {
			int size = src.remaining();
			openNewConnection(src, this.requestSeq.incrementAndGet());
			return size;
		}

		private void openNewConnection(final ByteBuffer payload, final long seq) {
			this.executor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						sendAndReceive(payload, seq);
					}
					catch (IOException ex) {
						logger.trace("Unexpected connection error", ex);
						closeQuitely();
					}
				}

				private void closeQuitely() {
					try {
						close();
					}
					catch (IOException ex) {
					}
				}

			});
		}

		private void sendAndReceive(ByteBuffer payload, long seq) throws IOException {
			ClientHttpRequest request = createRequest();
			if (payload != null) {
				sendPayload(payload, seq, request);
			}
			handleResponse(request.execute());
		}

		private void sendPayload(ByteBuffer payload, long seq, ClientHttpRequest request)
				throws IOException {
			HttpHeaders headers = request.getHeaders();
			headers.setContentLength(payload.remaining());
			headers.add(SEQ_HEADER, Long.toString(seq));
			WritableByteChannel body = Channels.newChannel(request.getBody());
			while (payload.hasRemaining()) {
				body.write(payload);
			}
			body.close();
		}

		private void handleResponse(ClientHttpResponse response) throws IOException {
			// FIXME payload
			// Confinue
			// disconnect
			if (response.getStatusCode() == HttpStatus.OK) {
				synchronized (this.incomingChannel) {
					forwardResponse(getPayload(response));
				}
			}
		}

		private ByteBuffer getPayload(ClientHttpResponse response) throws IOException {
			long length = response.getHeaders().getContentLength();
			Assert.state(length >= 0, "No content length provided");
			ReadableByteChannel body = Channels.newChannel(response.getBody());
			ByteBuffer payload = ByteBuffer.allocate((int) length);
			while (payload.hasRemaining()) {
				body.read(payload);
			}
			payload.flip();
			return payload;
		}

		private void forwardResponse(ByteBuffer payload) {

		}

	}

	private static class TunnelThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "HTTP Tunnel Connection");
			thread.setDaemon(true);
			return thread;
		}

	}

}
