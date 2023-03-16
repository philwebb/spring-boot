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
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import org.springframework.boot.devservices.dockercompose.interop.Port;
import org.springframework.boot.devservices.dockercompose.interop.Port.Protocol;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.test.RunningServiceBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TcpConnectServiceReadinessCheck}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class TcpConnectServiceReadinessCheckTests {

	private final TcpConnectServiceReadinessCheck readinessCheck = new TcpConnectServiceReadinessCheck(
			Duration.ofMillis(100), Duration.ofMillis(100));

	@Test
	void serverWritesData() throws IOException {
		withServer((socket) -> socket.getOutputStream().write('!'),
				(port) -> this.readinessCheck.check(createService(port)));
	}

	@Test
	void noSocketOutput() throws IOException {
		withServer((socket) ->
		// Simulate waiting for traffic from client to server. The sleep duration must
		// be longer than the read timeout of the ready check!
		sleep(Duration.ofSeconds(10)), (port) -> this.readinessCheck.check(createService(port)));
	}

	@Test
	void disconnectImmediately() throws IOException {
		withServer(Socket::close,
				(port) -> assertThatThrownBy(() -> this.readinessCheck.check(createService(port)))
					.isInstanceOf(ServiceNotReadyException.class)
					.hasMessageContaining("Immediately disconnect")
					.hasMessageContaining("%d/tcp".formatted(port)));
	}

	@ParameterizedTest
	@EnumSource(value = Protocol.class, names = "TCP", mode = Mode.EXCLUDE)
	void onlyUsesTcpPorts(Protocol protocol) {
		RunningService service = RunningServiceBuilder.create("service-1", "service:1")
			.addPort(12345, new Port(12345, protocol))
			.build();
		this.readinessCheck.check(service);
	}

	@Test
	void noServerListening() {
		assertThatThrownBy(() -> this.readinessCheck.check(createService(12345)))
			.isInstanceOf(ServiceNotReadyException.class)
			.hasMessageContaining("12345/tcp");
	}

	private void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			// Ignore
		}
	}

	private void withServer(ThrowingConsumer<Socket> socketHandler, ThrowingConsumer<Integer> callback)
			throws IOException {
		try (ServerSocket serverSocket = new ServerSocket()) {
			// 0 means random port
			serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
			Thread thread = new Thread(() -> {
				try (Socket socket = serverSocket.accept()) {
					socketHandler.accept(socket);
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			});
			thread.setName("Acceptor-%d".formatted(serverSocket.getLocalPort()));
			thread.setUncaughtExceptionHandler((ignored, ex) -> ex.printStackTrace());
			thread.setDaemon(true);
			thread.start();
			callback.accept(serverSocket.getLocalPort());
		}
	}

	private static RunningService createService(int... ports) {
		RunningServiceBuilder builder = RunningServiceBuilder.create("service-1", "service:1");
		for (int port : ports) {
			builder.addTcpPort(port, port);
		}
		return builder.build();
	}

}
