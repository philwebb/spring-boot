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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InetAddressMatcher} and {@link InternalInetAddressMatcher}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class InetAddressMatcherTests {

	private static InetAddressMatcherAssert assertThat(InetAddressMatcher matcher) {
		return new InetAddressMatcherAssert(matcher);
	}

	@Nested
	class MatchesStringTests {

		@Test
		void whenIpv4() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.1");
			assertThat(matcher).matchesString("192.168.1.1");
			assertThat(matcher).doesNotMatchString("192.168.1.2");
		}

		@Test
		void whenIpv6() {
			InetAddressMatcher matcher = InetAddressMatcher.of("fe80:0:0:0:21f:5bff:fe33:bd68");
			assertThat(matcher).matchesString("fe80::21f:5bff:fe33:bd68");
			assertThat(matcher).doesNotMatchString("fe90::21f:5bff:fe33:bd68");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void whenNull() {
			InetAddressMatcher matcher = (address) -> address != null;
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((String) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void whenNotAndIpAddress() {
			InetAddressMatcher matcher = InetAddressMatcher.all();
			assertThat(matcher).matches("192.168.1.1");
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches("not.an.ip.address"));
		}

		@Test
		void whenLambda() {
			InetAddressMatcher matcher = (address) -> address.getHostAddress().startsWith("192.168");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.100.200");
			assertThat(matcher).doesNotMatch("10.0.0.1");
		}

	}

	@Nested
	class MatchesSocketAddressTests {

		@Test
		void whenIpv4() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.1");
			assertThat(matcher).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("192.168.1.2", 8080));
		}

		@Test
		void whenIpv6() {
			InetAddressMatcher matcher = InetAddressMatcher.of("fe80:0:0:0:21f:5bff:fe33:bd68");
			assertThat(matcher).matches(new InetSocketAddress("fe80::21f:5bff:fe33:bd68", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("fe90::21f:5bff:fe33:bd68", 8080));
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void whenNull() {
			InetAddressMatcher matcher = (address) -> address != null;
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetSocketAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void whenLambda() {
			InetAddressMatcher matcher = (address) -> address.getHostAddress().startsWith("192.168");
			assertThat(matcher).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(matcher).matches(new InetSocketAddress("192.168.100.200", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("10.0.0.1", 8080));
		}

	}

	@Nested
	class AndTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressMatcher originalMatcher = (address) -> true;
			InetAddressMatcher matcher = originalMatcher.and(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/24");
			InetAddressMatcher matcher = originalMatcher.and("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/16");
			InetAddressMatcher matcher = originalMatcher.and("192.168.1.1/24", "192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.2.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void matchers() {
			InetAddressMatcher startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressMatcher endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressMatcher matcher = startsWithTen.and(endsWithOne);
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("10.0.0.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/24");
			InetAddressMatcher matcher = originalMatcher.and(List.of(InetAddressMatcher.of("192.168.1.1")));
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

	}

	@Nested
	class AndNotTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressMatcher originalMatcher = (address) -> true;
			InetAddressMatcher matcher = originalMatcher.andNot(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/24");
			InetAddressMatcher matcher = originalMatcher.andNot("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/24");
			InetAddressMatcher matcher = originalMatcher.andNot("192.168.1.1", "192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.3");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.3");
		}

		@Test
		void matchers() {
			InetAddressMatcher startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressMatcher endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressMatcher matcher = startsWithTen.andNot(endsWithOne);
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).matches("10.0.0.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1/24");
			InetAddressMatcher matcher = originalMatcher.andNot(List.of(InetAddressMatcher.of("192.168.1.1")));
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

	}

	@Nested
	class OrTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressMatcher originalMatcher = (address) -> true;
			InetAddressMatcher matcher = originalMatcher.or(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1");
			InetAddressMatcher matcher = originalMatcher.or("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1");
			InetAddressMatcher matcher = originalMatcher.or("192.168.1.2", "192.168.1.3");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(originalMatcher).doesNotMatch("192.168.1.3");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
			assertThat(matcher).matches("192.168.1.3");
		}

		@Test
		void matcher() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1");
			InetAddressMatcher matcher = originalMatcher.or(InetAddressMatcher.of("192.168.1.2"));
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1");
			InetAddressMatcher matcher = originalMatcher.or(List.of(InetAddressMatcher.of("192.168.1.2")));
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

	}

	@Nested
	class NegateTests {

		@Test
		void negate() {
			InetAddressMatcher originalMatcher = InetAddressMatcher.of("192.168.1.1");
			InetAddressMatcher matcher = originalMatcher.negate();
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

	}

	@Nested
	class ExternalAddressesTests {

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void nullInetAddress() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Public() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).matches("8.8.8.8");
			assertThat(matcher).matches("1.1.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).matches("2001:4860:4860::8888");
		}

		@Test
		void ipv4Private() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).doesNotMatch("172.16.0.1");
		}

		@Test
		void ipv4Loopback() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).doesNotMatch("127.0.0.1");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).doesNotMatch("169.254.0.0");
			assertThat(matcher).doesNotMatch("169.254.169.254");
			assertThat(matcher).doesNotMatch("169.254.255.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).doesNotMatch("::1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressMatcher matcher = InetAddressMatcher.externalAddresses();
			assertThat(matcher).doesNotMatch("fc00::1");
			assertThat(matcher).doesNotMatch("fd00::1");
		}

	}

	@Nested
	class InternalAddresses {

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void nullInetAddress() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Loopback() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("127.0.0.1");
			assertThat(matcher).matches("127.0.0.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("::1");
		}

		@Test
		void ipv4PrivateClass10() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).matches("10.255.255.255");
		}

		@Test
		void ipv4PrivateClass192() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("192.168.0.1");
			assertThat(matcher).matches("192.168.255.255");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("169.254.0.0");
			assertThat(matcher).matches("169.254.169.254");
			assertThat(matcher).matches("169.254.255.255");
		}

		@Test
		void ipv4PrivateClass172() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("172.16.0.1");
			assertThat(matcher).matches("172.16.255.255");
			assertThat(matcher).matches("172.17.1.1");
			assertThat(matcher).matches("172.31.255.255");
		}

		@Test
		void ipv4MappedIpv6Internal() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("::ffff:192.168.1.1");
			assertThat(matcher).matches("::ffff:169.254.169.254");
			assertThat(matcher).matches("::ffff:10.0.0.1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("fc00::1");
			assertThat(matcher).matches("fd00::1");
		}

		@Test
		void ipv6TranslationWithInternalIpv4() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("64:ff9b::10.0.0.1");
			assertThat(matcher).matches("64:ff9b::127.0.0.1");
			assertThat(matcher).matches("64:ff9b::192.168.1.1");
			assertThat(matcher).matches("64:ff9b::172.16.0.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith192ButNot168() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9b::192.0.2.1");
			assertThat(matcher).doesNotMatch("64:ff9b::192.167.1.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith172And16() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).matches("64:ff9b::172.16.0.1");
			assertThat(matcher).matches("64:ff9b::172.16.255.255");
		}

		@Test
		@ValueSource(strings = {})
		void ipv6TranslationWithExternalIpv4() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9b::8.8.8.8");
			assertThat(matcher).doesNotMatch("64:ff9b::1.1.1.1");
		}

		@Test
		void ppv6NonTranslationPrefixByte0() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("65:ff9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte1() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("64:fe9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte2() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9a::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte3() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9c::10.0.0.1");
		}

		@Test
		void ipv4Public() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("8.8.8.8");
			assertThat(matcher).doesNotMatch("1.1.1.1");
		}

		@Test
		void ipv4StartsWith192ButNot168() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("192.0.2.1");
			assertThat(matcher).doesNotMatch("192.167.1.1");
			assertThat(matcher).doesNotMatch("192.169.1.1");
		}

		@Test
		void ipv4StartsWith172ButNotPrivate16To31() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("172.15.1.1");
			assertThat(matcher).doesNotMatch("172.32.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses();
			assertThat(matcher).doesNotMatch("2001:4860:4860::8888");
		}

	}

	@Nested
	class NotTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressMatcher matcher = InetAddressMatcher.not(new String[] {});
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressMatcher matcher = InetAddressMatcher.not("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressMatcher matcher = InetAddressMatcher.not("192.168.1.1", "10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressMatcher matcher = InetAddressMatcher.not("192.168.1.0/24");
			assertThat(matcher).matches("192.168.2.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.255");
		}

		@Test
		void matchers() {
			InetAddressMatcher matcher = InetAddressMatcher.not(InetAddressMatcher.of("192.168.1.1"));
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressMatcher matcher = InetAddressMatcher.not(List.of(InetAddressMatcher.of("192.168.1.1")));
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

	}

	@Nested
	class OfTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressMatcher matcher = InetAddressMatcher.of(new String[] {});
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.1", "10.0.0.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.0/24");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.255");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void matcher() {
			InetAddressMatcher originalMatcher = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressMatcher matcher = InetAddressMatcher.of(originalMatcher);
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressMatcher originalMatcher = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressMatcher matcher = InetAddressMatcher.of(List.of(originalMatcher));
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

	}

	@Nested
	class CompositeTests {

		@Test
		void ofAndNot() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.0/24").andNot("192.168.1.100");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.100");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void ofOr() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.100").or("192.168.1.101");
			assertThat(matcher).matches("192.168.1.100");
			assertThat(matcher).matches("192.168.1.101");
			assertThat(matcher).doesNotMatch("192.168.1.102");
		}

		@Test
		void ofAnd() {
			InetAddressMatcher matcher = InetAddressMatcher.of("192.168.1.0/24")
				.and((address) -> address.getHostAddress().endsWith(".1"));
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void ofInternalAddressOrAndNot() {
			InetAddressMatcher matcher = InetAddressMatcher.internalAddresses()
				.or("8.8.8.8", "8.8.4.4")
				.andNot("192.168.2.0/24");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("8.8.8.8");
			assertThat(matcher).matches("8.8.4.4");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

	}

	@Nested
	class AllTests {

		@Test
		void all() {
			InetAddressMatcher matcher = InetAddressMatcher.all();
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void allWhenNull() {
			InetAddressMatcher matcher = InetAddressMatcher.all();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

	@Nested
	class NoneTests {

		@Test
		void none() {
			InetAddressMatcher matcher = InetAddressMatcher.none();
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void noneWhenNull() {
			InetAddressMatcher matcher = InetAddressMatcher.none();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

}
