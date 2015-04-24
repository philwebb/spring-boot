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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.StreamUtils;

public class HttpTunnelConnection implements TunnelConnection {

	private static final int BUFFER_SIZE = 1024 * 20;

	private static final ThreadFactory THREAD_FACTORY = new NamedThreadFactory(
			"HTTP tunnel connection", true);

	// FIXME we need a way for the server to disconnect and trigger the closeable

	private final URL url;

	public HttpTunnelConnection(String url) {
		try {
			this.url = new URL(url);
		}
		catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL " + url, ex);
		}
	}

	@Override
	public WritableByteChannel open(WritableByteChannel incomingChannel,
			Closeable closeable) throws Exception {
		return new ConnectionManager(incomingChannel, closeable);
	}

	private class ConnectionManager implements WritableByteChannel {

		private ExecutorService executor = Executors.newCachedThreadPool(THREAD_FACTORY);

		private final WritableByteChannel responseChannel;

		private AtomicInteger activeConnectionCount = new AtomicInteger();

		public ConnectionManager(WritableByteChannel responseChannel, Closeable closeable) {
			this.responseChannel = responseChannel;
			this.executor.execute(new Dunno(null));
		}

		@Override
		public boolean isOpen() {
			return true;
		}

		@Override
		public void close() throws IOException {
			// FIXME we need a way to pass this to the server and disconnect
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			System.out.println("> " + HexString.toString(src));
			log("Sending data " + src.remaining());
			int size = src.remaining();
			this.executor.execute(new Dunno(src));
			return size;
		}

		private class Dunno implements Runnable {

			private ByteBuffer output;

			public Dunno(ByteBuffer output) {
				this.output = output;
			}

			@Override
			public void run() {
				ConnectionManager.this.activeConnectionCount.incrementAndGet();
				try {
					sendAndRecieveData();
				}
				catch (IOException ex) {
					ex.printStackTrace(); // FIXME
				}
				finally {
					int active = ConnectionManager.this.activeConnectionCount
							.decrementAndGet();
					log("Now active " + active);
					if (active < 1) {
						ConnectionManager.this.executor.execute(new Dunno(null));
					}
				}
			}

			private void sendAndRecieveData() throws IOException {
				HttpURLConnection connection = openConnection();
				if (this.output != null) {
					synchronized (ConnectionManager.this.responseChannel) {
						connection.setDoOutput(true);
						log("Sending " + this.output.remaining());
						connection.setFixedLengthStreamingMode(this.output.remaining());
						OutputStream outputStream = connection.getOutputStream();
						WritableByteChannel request = Channels.newChannel(outputStream);
						while (this.output.hasRemaining()) {
							log("Writing to the data");
							request.write(this.output);
						}
						outputStream.close();
						this.output = null;
					}
				}
				else {
					log("Nothing to send");
				}
				InputStream inputStream = connection.getInputStream();
				try {
					forwardResponse(inputStream);
				}
				finally {
					inputStream.close();
				}
			}

			private void forwardResponse(InputStream inputStream) throws IOException {
				synchronized (ConnectionManager.this.responseChannel) {
					byte[] copyToByteArray = StreamUtils.copyToByteArray(inputStream);
					log("Got from remote " + copyToByteArray.length);
					ByteBuffer wrap = ByteBuffer.wrap(copyToByteArray);
					System.out.println("< " + HexString.toString(wrap));
					ConnectionManager.this.responseChannel.write(wrap);
				}
			}

			private HttpURLConnection openConnection() throws IOException {
				return (HttpURLConnection) HttpTunnelConnection.this.url.openConnection();
			}
		}

	}

	public static void log(String string) {
	}
}
