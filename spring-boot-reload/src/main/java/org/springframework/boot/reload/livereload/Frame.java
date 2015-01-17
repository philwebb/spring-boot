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

/**
 * A limited implementation of a WebSocket Frame used to carry LiveReload data.
 *
 * @author Phillip Webb
 */
class Frame {

	private static final byte[] NO_BYTES = new byte[0];

	private final Type type;

	private final byte[] payload;

	public Frame(String payload) {
		this(Type.TEXT, payload.getBytes());
	}

	public Frame(Type type) {
		this(type, NO_BYTES);
	}

	public Frame(Type type, byte[] payload) {
		this.type = type;
		this.payload = payload;
	}

	public Type getType() {
		return this.type;
	}

	public byte[] getPayload() {
		return this.payload;
	}

	@Override
	public String toString() {
		return new String(this.payload);
	}

	public void write(ConnectionOutputStream outputStream) throws IOException {
		outputStream.write(0x80 | this.type.code);
		if (this.payload.length < 126) {
			outputStream.write(0x00 | (this.payload.length & 0x7F));
		}
		else {
			outputStream.write(0x00 | (126 & 0x7F));
			outputStream.write(this.payload.length >> 8 & 0xFF);
			outputStream.write(this.payload.length >> 0 & 0xFF);
		}
		outputStream.write(this.payload);
		outputStream.flush();
	}

	public static Frame read(ConnectionInputStream inputStream) throws IOException {
		int firstByte = inputStream.checkedRead();
		if ((firstByte & 0x80) == 0) {
			throw new IllegalStateException("Fragmented frames are not supported");
		}
		int maskAndLength = inputStream.checkedRead();
		boolean hasMask = (maskAndLength & 0x80) != 0;
		int length = (maskAndLength & 0x7F);
		if (length == 126) {
			length = ((inputStream.checkedRead()) << 8 | inputStream.checkedRead());
		}
		else if (length == 127) {
			throw new IllegalStateException("Large frames are not supported");
		}
		byte[] mask = new byte[4];
		if (hasMask) {
			inputStream.readFully(mask, 0, mask.length);
		}
		byte[] payload = new byte[length];
		inputStream.readFully(payload, 0, length);
		if (hasMask) {
			for (int i = 0; i < payload.length; i++) {
				payload[i] ^= mask[i % 4];
			}
		}
		return new Frame(Type.forCode(firstByte & 0x0F), payload);
	}

	public static enum Type {

		CONTINUATION(0x00), TEXT(0x01), BINARY(0x02), CLOSE(0x08), PING(0x09), PONG(0x0A);

		private final int code;

		private Type(int code) {
			this.code = code;
		}

		public static Type forCode(int code) {
			for (Type type : values()) {
				if (type.code == code) {
					return type;
				}
			}
			throw new IllegalStateException("Unknown code " + code);
		}

	}

}
