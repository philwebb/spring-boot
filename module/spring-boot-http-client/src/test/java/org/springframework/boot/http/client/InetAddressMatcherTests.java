/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.security.util.matcher.InetAddressMatchers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InetAddressMatcher}.
 *
 * @author Phillip Webb
 */
class InetAddressMatcherTests {

	@Test
	void matchesSocketAddress() throws Exception {
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddressMatcher matcher = (address) -> Objects.equals(address, localhost);
		assertThat(matcher.matches(new InetSocketAddress(localhost, 8080))).isTrue();
		assertThat(matcher.matches(new InetSocketAddress(InetAddress.getByName("8.8.8.8"), 8080))).isFalse();
	}

	@Test
	void adaptSpringSecurity() throws Exception {
		InetAddressMatcher matcher = InetAddressMatcher.of(InetAddressMatchers.builder()
			.includeAddresses(List.of("192.168.0.0/24"))
			.excludeAddresses(List.of("192.168.0.1"))
			.build()::matches);
		assertThat(matcher.matches(InetAddress.getByName("192.168.0.1"))).isFalse();
		assertThat(matcher.matches(InetAddress.getByName("192.168.0.2"))).isTrue();
		assertThat(matcher.matches(InetAddress.getByName("192.168.0.3"))).isTrue();
	}

}
