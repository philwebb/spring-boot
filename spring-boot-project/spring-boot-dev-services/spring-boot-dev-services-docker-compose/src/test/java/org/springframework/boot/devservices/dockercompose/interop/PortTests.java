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

package org.springframework.boot.devservices.dockercompose.interop;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.interop.Port.Protocol;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Port}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class PortTests {

	@Test
	void parseTcp() {
		Assertions.assertThat(Protocol.parse("tcp")).isEqualTo(Protocol.TCP);
		assertThat(Protocol.parse("TCP")).isEqualTo(Protocol.TCP);
	}

	@Test
	void parseUdp() {
		assertThat(Protocol.parse("udp")).isEqualTo(Protocol.UDP);
		assertThat(Protocol.parse("UDP")).isEqualTo(Protocol.UDP);
	}

	@Test
	void parseOther() {
		assertThat(Protocol.parse("something")).isEqualTo(Protocol.OTHER);
	}

	@Test
	void testToString() {
		assertThat(new Port(80, Protocol.TCP)).hasToString("80/tcp");
		assertThat(new Port(51, Protocol.UDP)).hasToString("51/udp");
		assertThat(new Port(12345, Protocol.OTHER)).hasToString("12345/other");
	}

	@Test
	void parsePortWithProtocolTcp() {
		Port port = Port.parsePortWithProtocol("6379/tcp");
		assertThat(port.number()).isEqualTo(6379);
		assertThat(port.protocol()).isEqualTo(Protocol.TCP);
	}

	@Test
	void parsePortWithProtocolUdp() {
		Port port = Port.parsePortWithProtocol("6379/udp");
		assertThat(port.number()).isEqualTo(6379);
		assertThat(port.protocol()).isEqualTo(Protocol.UDP);
	}

	@Test
	void parsePortWithProtocolOther() {
		Port port = Port.parsePortWithProtocol("6379/something");
		assertThat(port.number()).isEqualTo(6379);
		assertThat(port.protocol()).isEqualTo(Protocol.OTHER);
	}

}
