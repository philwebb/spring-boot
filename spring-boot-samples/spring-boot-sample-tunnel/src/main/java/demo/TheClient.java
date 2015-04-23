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

import org.springframework.boot.livereload.tunnel.HttpTunnelConnection;
import org.springframework.boot.livereload.tunnel.TunnelClient;
import org.springframework.boot.livereload.tunnel.TunnelConnection;

/**
 * @author pwebb
 */
public class TheClient {

	public static void main(String[] args) throws Exception {
		// String url = "ws://localhost:8080/tunnel";
		// String url = "ws://192.168.1.56:8080/tunnel";
		// String url = "wss://tunnel.cfapps.io:4443/tunnel";
		String url = "http://localhost:8080/httptunnel";
		// TunnelConnection tunnelConnection = new WebSocketTunnelConnection(url);
		TunnelConnection tunnelConnection = new HttpTunnelConnection(url);
		TunnelClient client = new TunnelClient(8000, tunnelConnection);
		client.start();
		while (true) {
			Thread.sleep(1000);
		}
	}
}
