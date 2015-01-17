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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link OutputStream} for a server connection.
 *
 * @author Phillip Webb
 */
public class ConnectionOutputStream extends FilterOutputStream {

	private static final int BUFFER_SIZE = 1024 * 12;

	public ConnectionOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}

	public void writeHttp(InputStream content, String contentType) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[BUFFER_SIZE];
		int bytesRead = -1;
		while ((bytesRead = content.read(buffer)) != -1) {
			bytes.write(buffer, 0, bytesRead);
		}
		writeHeaders("HTTP/1.1 200 OK", "Content-Type: " + contentType,
				"Content-Length: " + bytes.toByteArray().length, "Connection: close");
		write(bytes.toByteArray());
		flush();
	}

	public void writeHeaders(String... headers) throws IOException {
		StringBuilder response1 = new StringBuilder();
		for (String header : headers) {
			response1.append(header).append("\r\n");
		}
		response1.append("\r\n");
		StringBuilder response = response1;
		write(response.toString().getBytes());
	}

}
