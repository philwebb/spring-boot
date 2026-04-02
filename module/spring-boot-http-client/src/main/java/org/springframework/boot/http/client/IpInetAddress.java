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
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An IP address and optional mask as used in Classless Inter-Domain Routing (CIDR).
 *
 * @param address the IP address
 * @param maskBitSize the mask size in bits
 * @author Luke Taylor
 * @author Steve Riesenberg
 * @author Andrey Litvitski
 * @author Rob Winch
 * @author Phillip Webb
 * @since 7.1
 */
record IpInetAddress(InetAddress address, int maskBitSize) {

	private static Pattern IPV4 = Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){0,3}(?:/\\d{1,2})?$");

	InetAddressMatcher matcher() {
		return this::matches;
	}

	private boolean matches(@Nullable InetAddress address) {
		if (address == null || this.maskBitSize < 0) {
			return this.address.equals(address);
		}
		if (this.maskBitSize == 0) {
			return true;
		}
		byte[] ours = this.address.getAddress();
		byte[] theirs = address.getAddress();
		return (ours.length == theirs.length) && matchesMasked(ours, theirs);
	}

	private boolean matchesMasked(byte[] ours, byte[] theirs) {
		boolean result = true;
		for (int i = 0; i < ours.length; i++) {
			int remain = Math.max(this.maskBitSize - (i * 8), 0);
			byte mask = (byte) ((remain < 8) ? 0xFF << (8 - remain) : 0xFF);
			result = result && (ours[i] & mask) == (theirs[i] & mask);
		}
		return result;
	}

	@Override
	public String toString() {
		String hostAddress = this.address.getHostAddress();
		String suffix = (this.maskBitSize > 0) ? "/" + this.maskBitSize : "";
		return "IpAddress [" + hostAddress + suffix + "]";
	}

	static IpInetAddress of(String address) {
		Assert.hasText(address, "'address' must not be empty");
		int slash = address.indexOf('/');
		if (slash == -1) {
			return new IpInetAddress(parseIpAddress(address), -1);
		}
		return of(address.substring(0, slash), parseMaskBitSize(address.substring(slash + 1)));
	}

	private static int parseMaskBitSize(String maskBitSize) {
		try {
			return Integer.parseInt(maskBitSize);
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("'address' subnet mask must be a number", ex);
		}
	}

	private static IpInetAddress of(String address, int maskBitSize) {
		InetAddress parsed = parseIpAddress(address);
		Assert.state(parsed != null, "'address' [%s] did not parse".formatted(address));
		Assert.isTrue(maskBitSize >= 0, "'maskBitSize' must be positive");
		Assert.isTrue(parsed.getAddress().length * 8 >= maskBitSize,
				() -> String.format("'address' [%s] is too short for bitmask of length %d", address, maskBitSize));
		return new IpInetAddress(parsed, maskBitSize);
	}

	/**
	 * Parses the given address string into an {@link InetAddress}.
	 * @param address the IP address string to parse
	 * @return the parsed {@link InetAddress}
	 * @throws IllegalArgumentException if the address cannot be parsed or appears to be a
	 * hostname
	 */
	static InetAddress parseIpAddress(String address) {
		Assert.isTrue(isLikelyIpAddress(address),
				() -> "'address' [%s] must be an IP address and not a host name".formatted(address));
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException ex) {
			throw new IllegalArgumentException("'address' [%s] must be parsable to an InetAddress".formatted(address),
					ex);
		}
	}

	private static boolean isLikelyIpAddress(String address) {
		return StringUtils.hasText(address) && (IPV4.matcher(address).matches() || isLikelyIpv6Address(address));
	}

	private static boolean isLikelyIpv6Address(String address) {
		char firstChar = address.charAt(0);
		return (firstChar == '[' || firstChar == ':') || (isHexDigit(firstChar) && address.contains(":"));
	}

	private static boolean isHexDigit(char ch) {
		return Character.digit(ch, 16) != -1;
	}

}
