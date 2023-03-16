/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.devservices.dockercompose.readiness;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;

import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * Checks service readiness by connecting to the exposed TCP ports.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class TcpConnectServiceReadinessCheck implements ServiceReadinessCheck {

	private final Duration connectTimeout;

	private final Duration readTimeout;

	TcpConnectServiceReadinessCheck(Duration connectTimeout, Duration readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	@Override
	public void check(RunningService service) {
		for (Port port : service.ports().values()) {
			if (port.protocol() == Port.Protocol.TCP) {
				try (Socket socket = new Socket()) {
					socket.setSoTimeout((int) this.readTimeout.toMillis());
					socket.connect(new InetSocketAddress(service.host(), port.number()),
							(int) this.connectTimeout.toMillis());
					boolean disconnected = read(socket);
					if (disconnected) {
						throw new ServiceNotReadyException(service,
								"Immediately disconnect while connecting to port %s".formatted(port));
					}
				}
				catch (IOException ex) {
					throw new ServiceNotReadyException(service,
							"IOException while connecting to port %s".formatted(port), ex);
				}
			}
		}
	}

	private static boolean read(Socket socket) throws IOException {
		try {
			// If -1 is returned, the socket has been closed immediately
			// In success case, the read either returns or a timeout exception is thrown
			return socket.getInputStream().read() == -1;
		}
		catch (SocketTimeoutException ex) {
			return false;
		}
	}

}
