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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * The client side component of a socket tunnel. Starts a {@link ServerSocket} of the
 * specified port for local clients to connect to.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class TunnelClient {

	private static final int BUFFER_SIZE = 1024 * 10;

	private static final Log logger = LogFactory.getLog(TunnelClient.class);

	private final int listenPort;

	private final TunnelConnection tunnelConnection;

	private ServerThread serverThread;

	public TunnelClient(int listenPort, TunnelConnection tunnelConnection) {
		Assert.isTrue(listenPort > 0, "ListenPort must be positive");
		Assert.notNull(tunnelConnection, "TunnelConnection must not be null");
		this.listenPort = listenPort;
		this.tunnelConnection = tunnelConnection;
	}

	/**
	 * Start the client and accept incoming connections on the port.
	 * @throws IOException
	 */
	public synchronized void start() throws IOException {
		Assert.state(this.serverThread == null, "Server already started");
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(this.listenPort));
		this.serverThread = new ServerThread(serverSocketChannel);
		this.serverThread.start();
	}

	/**
	 * Stop the client, disconnecting any servers.
	 * @throws IOException
	 */
	public synchronized void stop() throws IOException {
		if (this.serverThread != null) {
			this.serverThread.close();
			try {
				this.serverThread.join(2000);
			}
			catch (InterruptedException ex) {
			}
			this.serverThread = null;
		}
	}

	protected final ServerThread getServerThread() {
		return this.serverThread;
	}

	/**
	 * The main server thread.
	 */
	protected class ServerThread extends Thread {

		private final ServerSocketChannel serverSocketChannel;

		private boolean acceptConnections = true;

		public ServerThread(ServerSocketChannel serverSocketChannel) {
			this.serverSocketChannel = serverSocketChannel;
			setName("Tunnel Server");
			setDaemon(true);
		}

		public void close() throws IOException {
			this.serverSocketChannel.close();
			this.acceptConnections = false;
			interrupt();
		}

		@Override
		public void run() {
			try {
				while (this.acceptConnections) {
					SocketChannel socket = this.serverSocketChannel.accept();
					try {
						handleConnection(socket);
					}
					finally {
						socket.close();
					}
				}
			}
			catch (Exception ex) {
				logger.trace("Unexpected exception from tunnel client", ex);
			}
		}

		private void handleConnection(SocketChannel socketChannel) throws Exception {
			WritableByteChannel outputChannel = TunnelClient.this.tunnelConnection.open(
					socketChannel, socketChannel);
			try {
				while (true) {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					int amountRead = socketChannel.read(buffer);
					if (amountRead == -1) {
						outputChannel.close();
						return;
					}
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

		protected void stopAcceptingConnections() {
			this.acceptConnections = false;
		}
	}

}
