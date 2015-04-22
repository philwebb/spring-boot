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

package demo.old;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * @author pwebb
 */
public class TunnelWebSocketHandler extends BinaryWebSocketHandler {

	private final int port;

	private Socket socket;

	private OutputStream outputStream;

	public TunnelWebSocketHandler(int port) {
		this.port = port;
	}

	@Override
	public void afterConnectionEstablished(final WebSocketSession session)
			throws Exception {
		// FIXME check if already connected, perhaps close the other session
		this.socket = new Socket(InetAddress.getLocalHost(), this.port);
		this.outputStream = this.socket.getOutputStream();
		final InputStream inputStream = this.socket.getInputStream();
		Thread thread = new Thread() {
			@Override
			public void run() {
				while (TunnelWebSocketHandler.this.socket.isConnected()) {
					try {
						int available;
						available = inputStream.available();
						byte[] bytes = new byte[Math.max(1024,
								Math.min(available, 1024 * 20))];
						int read = inputStream.read(bytes);
						if (read > 0) {
							// System.out.println("Server read " + read);
							// System.out.println(HexUtils.toHexString(bytes));
							session.sendMessage(new BinaryMessage(bytes, 0, read, true));
						}
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
		};
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
			throws Exception {
		if (this.socket != null) {
			this.socket.close();
			this.socket = null;
		}
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
			throws Exception {
		byte[] bytes = message.getPayload().array();
		// System.out.println("Server write ");
		// System.out.println(HexUtils.toHexString(bytes));
		this.outputStream.write(bytes);
	}

}
