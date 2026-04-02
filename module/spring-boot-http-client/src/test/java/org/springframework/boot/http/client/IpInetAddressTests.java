/*
 * Copyright 2004-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link IpInetAddress}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class IpInetAddressTests {

	@Test
	void ofWhenAddressIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.of(null))
			.withMessage("'address' must not be empty");
	}

	@Test
	void ofWhenAddressIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.of(""))
			.withMessage("'address' must not be empty");
	}

	@Test
	void ofWithMaskWhenAddressIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.of("192.168.1.1/"))
			.withMessage("'address' subnet mask must be a number");
	}

	@Test
	void ofWhenUnmaskedIpAddress() throws Exception {
		IpInetAddress address = IpInetAddress.of("192.168.1.1");
		assertThat(address.address()).isEqualTo(InetAddress.getByName("192.168.1.1"));
		assertThat(address.maskBitSize()).isEqualTo(-1);
	}

	@Test
	void ofWhenMaskedIpAddress() throws Exception {
		IpInetAddress address = IpInetAddress.of("192.168.1.1/24");
		assertThat(address.address()).isEqualTo(InetAddress.getByName("192.168.1.1"));
		assertThat(address.maskBitSize()).isEqualTo(24);
	}

	@Test
	void parseIpAddressWhenIpv4() throws Exception {
		InetAddress parsed = IpInetAddress.parseIpAddress("192.168.1.1");
		assertThat(parsed).isEqualTo(InetAddress.getByName("192.168.1.1"));
	}

	@Test
	void parseIpAddressWhenIpv6InUrl() {
		InetAddress parsed = IpInetAddress.parseIpAddress("[::1]");
		assertThat(parsed.isLoopbackAddress()).isTrue();
	}

	@Test
	void parseIpAddressWhenIpv6Shortcut() {
		InetAddress parsed = IpInetAddress.parseIpAddress("::1");
		assertThat(parsed.isLoopbackAddress()).isTrue();
	}

	@Test
	void parseIpAddressWhenLikelyHost() {
		String message = "must be an IP address and not a host name";
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.parseIpAddress("https://example.com"))
			.withMessageContaining(message);
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.parseIpAddress("192.168.1.2.3"))
			.withMessageContaining(message);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> IpInetAddress.parseIpAddress("G001:0db8:0000:0000:0000:0000:0000:0000"))
			.withMessageContaining(message);
	}

	@Test
	void parseIpAddressWhenCannotBeParsed() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> IpInetAddress.parseIpAddress("2001:0db8:0000:0000:0000:0000:0000:000G"))
			.withMessageContaining("must be parsable to an InetAddress");
	}

	@Test
	void ofWithHostnameThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> IpInetAddress.of("example.com"))
			.withMessage("'address' [example.com] must be an IP address and not a host name");
	}

	@Test
	void matcherWhenUnmaskedIpv4() {
		IpInetAddress address = IpInetAddress.of("192.168.1.1");
		assertThatMatcher(address).matches("192.168.1.1");
		assertThatMatcher(address).doesNotMatch("192.168.1.2");
	}

	@Test
	void matcherWhenUnmaskedIpv6() {
		IpInetAddress address = IpInetAddress.of("fe80::21f:5bff:fe33:bd68");
		assertThatMatcher(address).matches("fe80::21f:5bff:fe33:bd68");
		assertThatMatcher(address).doesNotMatch("fe80::21f:5bff:fe33:bd69");
	}

	@Test
	void matcherWhenMaskedIpv4() {
		IpInetAddress address = IpInetAddress.of("192.168.1.0/24");
		assertThatMatcher(address).matches("192.168.1.1");
		assertThatMatcher(address).matches("192.168.1.255");
		assertThatMatcher(address).doesNotMatch("192.168.2.1");
		assertThatMatcher(address).doesNotMatch("192.168.0.255");
	}

	@Test
	void matcherWhenMaskedWithZero() {
		IpInetAddress address = IpInetAddress.of("192.168.1.0/0");
		assertThatMatcher(address).matches("192.168.1.1");
		assertThatMatcher(address).matches("192.168.1.255");
		assertThatMatcher(address).matches("8.8.8.8");
	}

	@Test
	void matcherWhenMaskedIpv6() {
		IpInetAddress address = IpInetAddress.of("2001:db8::/48");
		assertThatMatcher(address).matches("2001:db8:0:0:0:0:0:0");
		assertThatMatcher(address).matches("2001:db8:0:ffff:ffff:ffff:ffff:ffff");
		assertThatMatcher(address).doesNotMatch("2001:db8:1:0:0:0:0:0");
	}

	@Test
	void matcherWhenMaskedIpv4OutsideOfByteBoundary() {
		IpInetAddress address = IpInetAddress.of("192.168.1.0/30");
		assertThatMatcher(address).matches("192.168.1.0");
		assertThatMatcher(address).matches("192.168.1.1");
		assertThatMatcher(address).matches("192.168.1.2");
		assertThatMatcher(address).matches("192.168.1.3");
		assertThatMatcher(address).doesNotMatch("192.168.1.4");
	}

	@Test
	void matcherWhenIpv4DoesNotMatchIpv6() {
		IpInetAddress address = IpInetAddress.of("192.168.1.1");
		assertThatMatcher(address).doesNotMatch("fe80::21f:5bff:fe33:bd68");
	}

	@Test
	void matcherWhenIpv6DoesNotMatchIpv4() {
		IpInetAddress address = IpInetAddress.of("fe80::21f:5bff:fe33:bd68");
		assertThatMatcher(address).doesNotMatch("192.168.1.1");
	}

	@Test
	void matcherWhenCheckingNullDoesNotMatch() {
		IpInetAddress address = IpInetAddress.of("192.168.1.1");
		assertThatMatcher(address).doesNotMatch((InetAddress) null);
	}

	@Test
	void matcherWhenMatchingString() {
		IpInetAddress address = IpInetAddress.of("192.168.1.1");
		assertThatMatcher(address).matchesString("192.168.1.1");
		assertThatMatcher(address).doesNotMatchString("192.168.1.2");
		assertThatMatcher(address).doesNotMatchString(null);
	}

	private static InetAddressMatcherAssert assertThatMatcher(IpInetAddress address) {
		return new InetAddressMatcherAssert(address.matcher());
	}

}
