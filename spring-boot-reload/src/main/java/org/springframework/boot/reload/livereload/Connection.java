/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.reload.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.reload.livereload.Frame.Type;
import org.springframework.boot.reload.log.Log;

/**
 * A {@link LiveReloadServer} connection.
 */
class Connection {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final Pattern WEBSOCKET_KEY_PATTERN = Pattern.compile(
			"^Sec-WebSocket-Key:(.*)$", Pattern.MULTILINE);

	public final static String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private final Socket socket;

	private final ConnectionInputStream inputStream;

	private final ConnectionOutputStream outputStream;

	private final String header;

	private boolean webSocket;

	public Connection(Socket socket, InputStream inputStream, OutputStream outputStream)
			throws IOException {
		this.socket = socket;
		this.inputStream = new ConnectionInputStream(inputStream);
		this.outputStream = new ConnectionOutputStream(outputStream);
		this.header = this.inputStream.readHeader();
	}

	public void run() throws Exception {
		if (this.header.contains("Upgrade: websocket")
				&& this.header.contains("Sec-WebSocket-Version: 13")) {
			runWebSocket(this.header);
		}
		if (this.header.contains("GET /livereload.js")) {
			this.outputStream.writeHttp(getClass().getResourceAsStream("livereload.js"),
					"text/javascript");
		}
	}

	public void triggerReload() throws IOException {
		// FIXME paths
		if (this.webSocket) {
			Log.debug("Triggering LiveReload");
			writeWebSocketFrame(new Frame("{\"command\":\"reload\",\"path\":\"/\"}"));
		}
	}

	private void runWebSocket(String header) throws Exception {
		String accept = getWebsocketAcceptResponse();
		this.outputStream.writeHeaders("HTTP/1.1 101 Switching Protocols",
				"Upgrade: websocket", "Connection: Upgrade", "Sec-WebSocket-Accept: "
						+ accept);
		new Frame("{\"command\":\"hello\",\"protocols\":"
				+ "[\"http://livereload.com/protocols/official-7\"],"
				+ "\"serverName\":\"spring-boot\"}").write(this.outputStream);
		this.webSocket = true;
		while (!this.socket.isClosed()) {
			readWebSocketFrame();
		}
	}

	private void readWebSocketFrame() throws IOException {
		try {
			Frame frame = Frame.read(this.inputStream);
			if (frame.getType() == Frame.Type.PING) {
				writeWebSocketFrame(new Frame(Type.PONG));
			}
			else if (frame.getType() == Frame.Type.CLOSE) {
				throw new ConnectionClosedException();
			}
			else if (frame.getType() == Frame.Type.TEXT) {
				Log.debug("Recieved LiveReload text frame " + frame);
			}
			else {
				throw new IOException("Unexpected Frame Type " + frame.getType());
			}
		}
		catch (SocketTimeoutException ex) {
			writeWebSocketFrame(new Frame(Type.PING));
			Frame frame = Frame.read(this.inputStream);
			if (frame.getType() != Frame.Type.PONG) {
				throw new IllegalStateException("No Pong");
			}
		}
	}

	private synchronized void writeWebSocketFrame(Frame frame) throws IOException {
		frame.write(this.outputStream);
	}

	private String getWebsocketAcceptResponse() throws NoSuchAlgorithmException {
		Matcher matcher = WEBSOCKET_KEY_PATTERN.matcher(this.header);
		if (!matcher.find()) {
			throw new IllegalStateException("No Sec-WebSocket-Key");
		}
		String response = matcher.group(1).trim() + WEBSOCKET_GUID;
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		messageDigest.update(response.getBytes(), 0, response.length());
		// FIXME SPR-12938
		return new String(Base64.encode(messageDigest.digest()), DEFAULT_CHARSET);
	}
}
