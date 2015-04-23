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
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Adapts a {@link WebSocketSession} to a {@link WritableByteChannel}.
 *
 * @author Phillip Webb
 */
public class WebSocketSessionChannel implements WritableByteChannel {

	private WebSocketSession session;

	public WebSocketSessionChannel(WebSocketSession session) {
		this.session = session;
	}

	@Override
	public boolean isOpen() {
		return this.session.isOpen();
	}

	@Override
	public void close() throws IOException {
		this.session.close();
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int remaining = src.remaining();
		this.session.sendMessage(new BinaryMessage(src));
		return remaining;
	}

}
