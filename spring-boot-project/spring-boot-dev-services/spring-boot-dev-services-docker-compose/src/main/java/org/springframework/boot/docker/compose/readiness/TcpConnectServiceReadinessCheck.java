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

package org.springframework.boot.docker.compose.readiness;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.springframework.boot.docker.compose.service.DockerComposeRunningService;
import org.springframework.boot.docker.compose.service.Port;

/**
 * Default {@link ServiceReadinessCheck} that readiness by connecting to the exposed TCP
 * ports.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class TcpConnectServiceReadinessCheck implements ServiceReadinessCheck {

	private final String DISABLE_LABEL = "org.springframework.boot.readiness-check.tcp.disable";

	private final ReadinessProperties.Tcp properties;

	TcpConnectServiceReadinessCheck(ReadinessProperties.Tcp properties) {
		this.properties = properties;
	}

	@Override
	public void check(DockerComposeRunningService service) {
		if (service.labels().containsKey(this.DISABLE_LABEL)) {
			return;
		}
		for (Port port : service.ports().values()) {
			if (port.protocol() == Port.Protocol.TCP) {
				check(service, port);
			}
		}
	}

	private void check(DockerComposeRunningService service, Port port) {
		int connectTimeout = (int) this.properties.getConnectTimeout().toMillis();
		int readTimeout = (int) this.properties.getReadTimeout().toMillis();
		try (Socket socket = new Socket()) {
			socket.setSoTimeout(readTimeout);
			socket.connect(new InetSocketAddress(service.host(), port.number()), connectTimeout);
			check(service, port, socket);
		}
		catch (IOException ex) {
			throw new ServiceNotReadyException(service, "IOException while connecting to port %s".formatted(port), ex);
		}
	}

	private void check(DockerComposeRunningService service, Port port, Socket socket) throws IOException {
		try {
			// -1 is indicates the socket has been closed immediately
			// Other responses or a timeout are considered as success
			if (socket.getInputStream().read() == -1) {
				throw new ServiceNotReadyException(service,
						"Immediate disconnect while connecting to port %s".formatted(port));
			}
		}
		catch (SocketTimeoutException ex) {
		}
	}

}
