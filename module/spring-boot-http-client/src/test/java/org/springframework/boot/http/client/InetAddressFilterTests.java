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
 * Tests for {@link InetAddressFilter} and {@link InternalInetAddressFilter}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
class InetAddressFilterTests {

	private static InetAddressMatcherAssert assertThat(InetAddressFilter matcher) {
		return new InetAddressMatcherAssert(matcher);
	}

	@Nested
	class MatchesSocketAddressTests {

		@Test
		void whenIpv4() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.1");
			assertThat(matcher).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("192.168.1.2", 8080));
		}

		@Test
		void whenIpv6() {
			InetAddressFilter matcher = InetAddressFilter.of("fe80:0:0:0:21f:5bff:fe33:bd68");
			assertThat(matcher).matches(new InetSocketAddress("fe80::21f:5bff:fe33:bd68", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("fe90::21f:5bff:fe33:bd68", 8080));
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void whenNull() {
			InetAddressFilter matcher = (address) -> address != null;
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetSocketAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void whenLambda() {
			InetAddressFilter matcher = (address) -> address.getHostAddress().startsWith("192.168");
			assertThat(matcher).matches(new InetSocketAddress("192.168.1.1", 8080));
			assertThat(matcher).matches(new InetSocketAddress("192.168.100.200", 8080));
			assertThat(matcher).doesNotMatch(new InetSocketAddress("10.0.0.1", 8080));
		}

	}

	@Nested
	class AndTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter originalMatcher = (address) -> true;
			InetAddressFilter matcher = originalMatcher.and(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter matcher = originalMatcher.and("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/16");
			InetAddressFilter matcher = originalMatcher.and("192.168.1.1/24", "192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.2.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void matchers() {
			InetAddressFilter startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressFilter matcher = startsWithTen.and(endsWithOne);
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("10.0.0.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter matcher = originalMatcher.and(List.of(InetAddressFilter.of("192.168.1.1")));
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
			InetAddressFilter originalMatcher = (address) -> true;
			InetAddressFilter matcher = originalMatcher.andNot(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter matcher = originalMatcher.andNot("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter matcher = originalMatcher.andNot("192.168.1.1", "192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).matches("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.3");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.3");
		}

		@Test
		void matchers() {
			InetAddressFilter startsWithTen = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter endsWithOne = (address) -> address.getHostAddress().endsWith(".1");
			InetAddressFilter matcher = startsWithTen.andNot(endsWithOne);
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).matches("10.0.0.2");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1/24");
			InetAddressFilter matcher = originalMatcher.andNot(List.of(InetAddressFilter.of("192.168.1.1")));
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
			InetAddressFilter originalMatcher = (address) -> true;
			InetAddressFilter matcher = originalMatcher.or(new String[] {});
			assertThat(matcher).isSameAs(originalMatcher);
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter matcher = originalMatcher.or("192.168.1.2");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter matcher = originalMatcher.or("192.168.1.2", "192.168.1.3");
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(originalMatcher).doesNotMatch("192.168.1.3");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
			assertThat(matcher).matches("192.168.1.3");
		}

		@Test
		void matcher() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter matcher = originalMatcher.or(InetAddressFilter.of("192.168.1.2"));
			assertThat(originalMatcher).matches("192.168.1.1");
			assertThat(originalMatcher).doesNotMatch("192.168.1.2");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter matcher = originalMatcher.or(List.of(InetAddressFilter.of("192.168.1.2")));
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
			InetAddressFilter originalMatcher = InetAddressFilter.of("192.168.1.1");
			InetAddressFilter matcher = originalMatcher.negate();
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
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Public() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).matches("8.8.8.8");
			assertThat(matcher).matches("1.1.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).matches("2001:4860:4860::8888");
		}

		@Test
		void ipv4Private() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).doesNotMatch("172.16.0.1");
		}

		@Test
		void ipv4Loopback() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("127.0.0.1");
			assertThat(matcher).doesNotMatch("127.1.1.1");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("169.254.0.0");
			assertThat(matcher).doesNotMatch("169.254.169.254");
			assertThat(matcher).doesNotMatch("169.254.255.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("::1");
			assertThat(matcher).doesNotMatch("0000::1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("fc00::1");
			assertThat(matcher).doesNotMatch("fd00::1");
		}

		@Test
		void ipv4NonRoutable() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("0.0.0.0");
		}

		@Test
		void ipv6NonRoutable() {
			InetAddressFilter matcher = InetAddressFilter.externalAddresses();
			assertThat(matcher).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(matcher).doesNotMatch("::");
		}

	}

	@Nested
	class InternalAddresses {

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void nullInetAddress() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

		@Test
		void ipv4Loopback() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("127.0.0.1");
			assertThat(matcher).matches("127.1.1.1");
			assertThat(matcher).matches("127.0.0.255");
		}

		@Test
		void ipv6Loopback() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("::1");
			assertThat(matcher).matches("0000::1");
		}

		@Test
		void ipv4PrivateClass10() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).matches("10.255.255.255");
		}

		@Test
		void ipv4PrivateClass192() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("192.168.0.1");
			assertThat(matcher).matches("192.168.255.255");
		}

		@Test
		void ipv4LinkLocal() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("169.254.0.0");
			assertThat(matcher).matches("169.254.169.254");
			assertThat(matcher).matches("169.254.255.255");
		}

		@Test
		void ipv4PrivateClass172() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("172.16.0.1");
			assertThat(matcher).matches("172.16.255.255");
			assertThat(matcher).matches("172.17.1.1");
			assertThat(matcher).matches("172.31.255.255");
		}

		@Test
		void ipv4MappedIpv6Internal() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("::ffff:127.0.0.1");
			assertThat(matcher).matches("::ffff:192.168.1.1");
			assertThat(matcher).matches("::ffff:169.254.169.254");
			assertThat(matcher).matches("::ffff:10.0.0.1");
		}

		@Test
		void ipv6UniqueLocal() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("fc00::1");
			assertThat(matcher).matches("fd00::1");
		}

		@Test
		void ipv6TranslationWithInternalIpv4() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("64:ff9b::10.0.0.1");
			assertThat(matcher).matches("64:ff9b::127.0.0.1");
			assertThat(matcher).matches("64:ff9b::192.168.1.1");
			assertThat(matcher).matches("64:ff9b::172.16.0.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith192ButNot168() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9b::192.0.2.1");
			assertThat(matcher).doesNotMatch("64:ff9b::192.167.1.1");
		}

		@Test
		void ipv6TranslationWithIpv4StartsWith172And16() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).matches("64:ff9b::172.16.0.1");
			assertThat(matcher).matches("64:ff9b::172.16.255.255");
		}

		@Test
		@ValueSource(strings = {})
		void ipv6TranslationWithExternalIpv4() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9b::8.8.8.8");
			assertThat(matcher).doesNotMatch("64:ff9b::1.1.1.1");
		}

		@Test
		void ppv6NonTranslationPrefixByte0() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("65:ff9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte1() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("64:fe9b::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte2() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9a::10.0.0.1");
		}

		@Test
		void ipv6NonTranslationPrefixByte3() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("64:ff9c::10.0.0.1");
		}

		@Test
		void ipv4Public() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("8.8.8.8");
			assertThat(matcher).doesNotMatch("1.1.1.1");
		}

		@Test
		void ipv4StartsWith192ButNot168() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("192.0.2.1");
			assertThat(matcher).doesNotMatch("192.167.1.1");
			assertThat(matcher).doesNotMatch("192.169.1.1");
		}

		@Test
		void ipv4StartsWith172ButNotPrivate16To31() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("172.15.1.1");
			assertThat(matcher).doesNotMatch("172.32.1.1");
		}

		@Test
		void ipv6Public() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("2001:4860:4860::8888");
		}

		@Test
		void ipv4NonRoutable() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("0.0.0.0");
		}

		@Test
		void ipv6NonRoutable() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses();
			assertThat(matcher).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(matcher).doesNotMatch("::");
		}

	}

	@Nested
	class Routable {

		@Test
		void nonRoutable() {
			InetAddressFilter matcher = InetAddressFilter.routable();
			assertThat(matcher).doesNotMatch("0.0.0.0");
			assertThat(matcher).doesNotMatch("0000:0000:0000:0000:0000:0000:0000:0000");
			assertThat(matcher).doesNotMatch("::");

		}

		@Test
		void routable() {
			InetAddressFilter matcher = InetAddressFilter.routable();
			assertThat(matcher).matches("0.0.0.1");
			assertThat(matcher).matches("0000:0000:0000:0000:0000:0000:0000:0001");
		}

	}

	@Nested
	class NotTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter matcher = InetAddressFilter.not(new String[] {});
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter matcher = InetAddressFilter.not("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter matcher = InetAddressFilter.not("192.168.1.1", "10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("10.0.0.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressFilter matcher = InetAddressFilter.not("192.168.1.0/24");
			assertThat(matcher).matches("192.168.2.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.255");
		}

		@Test
		void matchers() {
			InetAddressFilter matcher = InetAddressFilter.not(InetAddressFilter.of("192.168.1.1"));
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

		@Test
		void collection() {
			InetAddressFilter matcher = InetAddressFilter.not(List.of(InetAddressFilter.of("192.168.1.1")));
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).matches("192.168.1.2");
		}

	}

	@Nested
	class OfTests {

		@Test
		void stringsWhenEmpty() {
			InetAddressFilter matcher = InetAddressFilter.of(new String[] {});
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenSingle() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void stringsWhenMultiple() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.1", "10.0.0.1");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		void stringsWhenCidr() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.0/24");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("192.168.1.255");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void matcher() {
			InetAddressFilter originalMatcher = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter matcher = InetAddressFilter.of(originalMatcher);
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

		@Test
		void collection() {
			InetAddressFilter originalMatcher = (address) -> address.getHostAddress().startsWith("10.");
			InetAddressFilter matcher = InetAddressFilter.of(List.of(originalMatcher));
			assertThat(matcher).matches("10.0.0.1");
			assertThat(matcher).doesNotMatch("192.168.1.1");
		}

	}

	@Nested
	class CompositeTests {

		@Test
		void ofAndNot() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.0/24").andNot("192.168.1.100");
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.100");
			assertThat(matcher).doesNotMatch("192.168.2.1");
		}

		@Test
		void ofOr() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.100").or("192.168.1.101");
			assertThat(matcher).matches("192.168.1.100");
			assertThat(matcher).matches("192.168.1.101");
			assertThat(matcher).doesNotMatch("192.168.1.102");
		}

		@Test
		void ofAnd() {
			InetAddressFilter matcher = InetAddressFilter.of("192.168.1.0/24")
				.and((address) -> address.getHostAddress().endsWith(".1"));
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).doesNotMatch("192.168.1.2");
		}

		@Test
		void ofInternalAddressOrAndNot() {
			InetAddressFilter matcher = InetAddressFilter.internalAddresses()
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
			InetAddressFilter matcher = InetAddressFilter.all();
			assertThat(matcher).matches("192.168.1.1");
			assertThat(matcher).matches("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void allWhenNull() {
			InetAddressFilter matcher = InetAddressFilter.all();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

	@Nested
	class NoneTests {

		@Test
		void none() {
			InetAddressFilter matcher = InetAddressFilter.none();
			assertThat(matcher).doesNotMatch("192.168.1.1");
			assertThat(matcher).doesNotMatch("8.8.8.8");
		}

		@Test
		@SuppressWarnings("NullAway") // Test null check
		void noneWhenNull() {
			InetAddressFilter matcher = InetAddressFilter.none();
			assertThatIllegalArgumentException().isThrownBy(() -> matcher.matches((InetAddress) null))
				.withMessage("'address' must not be null");
		}

	}

}
