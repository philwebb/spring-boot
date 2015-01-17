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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} for a server connection.
 *
 * @author Phillip Webb
 */
public class ConnectionInputStream extends FilterInputStream {

	private static final String HEADER_END = "\r\n\r\n";

	private static final int BUFFER_SIZE = 1024 * 12;

	public ConnectionInputStream(InputStream in) {
		super(in);
	}

	public String readHeader() throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		StringBuffer content = new StringBuffer(BUFFER_SIZE);
		while (content.indexOf(HEADER_END) == -1) {
			int amountRead = checkedRead(buffer, 0, BUFFER_SIZE);
			content.append(new String(buffer, 0, amountRead));
		}
		return content.toString();
	}

	public void readFully(byte[] buffer, int offset, int length) throws IOException {
		while (length > 0) {
			int amountRead = checkedRead(buffer, offset, length);
			offset += amountRead;
			length -= amountRead;
		}
	}

	public int checkedRead() throws IOException {
		int b = read();
		if (b == -1) {
			throw new IOException("End of stream");
		}
		return (b & 0xff);
	}

	public int checkedRead(byte[] buffer, int offset, int length) throws IOException {
		int amountRead = read(buffer, offset, length);
		if (amountRead == -1) {
			throw new IOException();
		}
		return amountRead;
	}

}
