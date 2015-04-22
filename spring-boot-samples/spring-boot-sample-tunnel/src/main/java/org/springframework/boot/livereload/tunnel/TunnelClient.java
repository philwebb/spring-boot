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
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.springframework.util.Assert;

/**
 * The client side component of a socket tunnel. Starts a {@link ServerSocket} of the
 * specified port for local clients to connect to.
 *
 * @author Phillip Webb
 */
public class TunnelClient {

	private static final int BUFFER_SIZE = 1025 * 4;

	private final int listenPort;

	private final TunnelConnection tunnelConnection;

	private Server server;

	public TunnelClient(int listenPort, TunnelConnection tunnelConnection) {
		this.listenPort = listenPort;
		this.tunnelConnection = tunnelConnection;
	}

	public synchronized void start() throws IOException {
		Assert.state(this.server == null, "Server already started");
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(this.listenPort));
		this.server = new Server(serverSocketChannel);
		Thread thread = new Thread(this.server, "WebSocketTunnelClient");
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized void stop() throws IOException {
		if (this.server != null) {
			this.server.close();
			this.server = null;
		}
	}

	private class Server implements Runnable {

		private final ServerSocketChannel serverSocketChannel;

		public Server(ServerSocketChannel serverSocketChannel) {
			this.serverSocketChannel = serverSocketChannel;
		}

		public void close() throws IOException {
			this.serverSocketChannel.close();
		}

		@Override
		public void run() {
			try {
				SocketChannel socket = this.serverSocketChannel.accept();
				try {
					handleConnection(socket);
				}
				finally {
					socket.close();
				}
			}
			catch (Exception ex) {
				// FIXME
				ex.printStackTrace();
			}
		}

		private void handleConnection(SocketChannel socketChannel) throws Exception {
			TunnelConnection connection = TunnelClient.this.tunnelConnection;
			WritableByteChannel outputChannel = connection.open(socketChannel,
					socketChannel);
			try {
				while (true) {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					int amountRead = socketChannel.read(buffer);
					Assert.state(amountRead >= 0, "Socket channel closed");
					if (amountRead > 0) {
						buffer.flip();
						outputChannel.write(buffer);
					}
				}
			}
			finally {
				outputChannel.close();
			}
		}

	}

}
