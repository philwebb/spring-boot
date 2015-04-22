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
import java.net.Socket;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * @author pwebb
 */
public class TunnelWebSocketClientHandler extends BinaryWebSocketHandler {

	private int port;

	private OutputStream outputStream;

	private Socket socket;

	private InputStream inputStream;

	public TunnelWebSocketClientHandler(int port) {
		this.port = port;
	}

	public TunnelWebSocketClientHandler(Socket socket, InputStream inputStream)
			throws IOException {
		this.socket = socket;
		this.inputStream = inputStream;
		this.outputStream = socket.getOutputStream();
	}

	@Override
	public void afterConnectionEstablished(final WebSocketSession session)
			throws Exception {
		System.out.println("Client Est");
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runConnection(session);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			};
		};
		t.setDaemon(false);
		t.start();
	}

	private void runConnection(WebSocketSession session) throws IOException, Exception {
		while (this.socket.isConnected()) {
			int available;
			available = this.inputStream.available();
			byte[] bytes = new byte[Math.max(1024, Math.min(available, 1024 * 20))];
			int read = this.inputStream.read(bytes);
			if (read > 0) {
				// System.out.println("Client read " + read);
				// System.out.println(HexUtils.toHexString(bytes));
				session.sendMessage(new BinaryMessage(bytes, 0, read, true));
			}
		}
	}

	@Override
	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
			throws Exception {
		byte[] bytes = message.getPayload().array();
		// System.out.println("Client write " + bytes.length);
		// System.out.println(HexUtils.toHexString(bytes));
		this.outputStream.write(bytes);
	}

}
