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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

/**
 * {@link WebSocketHttpRequestHandler} for the server side component of a socket tunnel.
 *
 * @author Phillip Webb
 */
public class TunnelServerWebSocketHandler extends BinaryWebSocketHandler {

	private static final int BUFFER_SIZE = 1025 * 2;

	private final int connectionPort;

	private Client client;

	public TunnelServerWebSocketHandler(int connectionPort) {
		this.connectionPort = connectionPort;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		SocketAddress address = new InetSocketAddress(this.connectionPort);
		SocketChannel socketChannel = SocketChannel.open(address);
		WebSocketSessionChannel sessionChannel = new WebSocketSessionChannel(session);
		this.client = new Client(socketChannel, sessionChannel);
		Thread thread = new Thread(this.client, "Tunnel Connection");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
			throws Exception {
		this.client.close();
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
			throws Exception {
		this.client.write(message.getPayload());
	}

	private static class Client implements Runnable {

		private final SocketChannel socketChannel;

		private final WritableByteChannel outputChannel;

		public Client(SocketChannel socketChannel, WritableByteChannel outputChannel) {
			this.socketChannel = socketChannel;
			this.outputChannel = outputChannel;
		}

		@Override
		public void run() {
			try {
				try {
					copyBytes();
				}
				finally {
					close();
				}
			}
			catch (IOException ex) {
			}
		}

		private void copyBytes() throws IOException {
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				int amountRead = this.socketChannel.read(buffer);
				Assert.state(amountRead >= 0, "Input channel closed");
				if (amountRead > 0) {
					buffer.flip();
					this.outputChannel.write(buffer);
				}
			}
		}

		public void write(ByteBuffer payload) throws IOException {
			this.socketChannel.write(payload);
		}

		public void close() throws IOException {
			this.outputChannel.close();
		}

	}

}
