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

package demo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author pwebb
 */
public class StreamTest {

	public static void main(String[] args) throws IOException, InterruptedException {

		// URL url = new URL("http://localhost:8080/streamin");
		URL url = new URL("http://tunnel.cfapps.io/streamin");
		HttpURLConnection openConnection = (HttpURLConnection) url.openConnection();
		openConnection.setFixedLengthStreamingMode(100000);
		openConnection.setUseCaches(false);
		openConnection.setDoOutput(true);
		OutputStream outputStream = openConnection.getOutputStream();
		for (int i = 0; i < 100; i++) {
			String d = "Package " + i;
			outputStream.write(d.getBytes());
			outputStream.flush();
			Thread.sleep(400);
		}
		outputStream.close();
		//
		// Socket socket = new Socket("localhost", 8080);
		// OutputStream outputStream = socket.getOutputStream();
		// outputStream.write("POST /streamin HTTP/1.0\r\n\r\n\r\n".getBytes());
		// outputStream.flush();
		// outputStream.write("Hello".getBytes());
		// outputStream.close();
		// socket.close();
	}
}
