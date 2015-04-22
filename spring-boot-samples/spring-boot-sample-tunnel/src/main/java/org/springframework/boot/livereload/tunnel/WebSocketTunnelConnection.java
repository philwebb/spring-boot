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
import java.nio.channels.WritableByteChannel;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * A WebSocket based {@link TunnelConnection}.
 *
 * @author Phillip Webb
 */
public class WebSocketTunnelConnection implements TunnelConnection {

	private final String url;

	private final WebSocketClient client;

	public WebSocketTunnelConnection(String url) {
		this(url, new StandardWebSocketClient());
	}

	public WebSocketTunnelConnection(String address, WebSocketClient client) {
		this.url = address;
		this.client = client;
	}

	@Override
	public WritableByteChannel open(WritableByteChannel incomingChannel,
			Closeable closeable) throws Exception {
		IncomingDataHandler handler = new IncomingDataHandler(incomingChannel, closeable);
		WebSocketSession session = this.client.doHandshake(handler, this.url).get();
		return new WebSocketSessionChannel(session);
	}

	private static class IncomingDataHandler extends BinaryWebSocketHandler {

		private final WritableByteChannel channel;

		private final Closeable closeable;

		public IncomingDataHandler(WritableByteChannel channel, Closeable closeable) {
			this.channel = channel;
			this.closeable = closeable;
		}

		@Override
		protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
				throws Exception {
			// System.out.println("Received " + message.getPayloadLength());
			this.channel.write(message.getPayload());
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
				throws Exception {
			this.closeable.close();
		}

	}

}
