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
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Strategy interface used for {@link InetAddress}-based matching.
 * <p>
 * Allow HTTP clients to offer Server-Side Request Forgery (SSRF) mitigation features, for
 * example by only allowing local addresses to be called.
 * <p>
 * Matchers are typically adapted from Spring Security
 * {@code org.springframework.security.web.util.matcher.InetAddressMatcher} built
 * instances. For example: <pre class="code">
 * InetAddressMatcher matcher = InetAddressMatcher.of(InetAddressMatchers.builder()
 * 		.includeAddresses(List.of("192.168.0.0/24"))
 * 		.excludeAddresses(List.of("192.168.0.1"))
 * 		.build()::matches);
 * </pre>
 *
 * @author Phillip Webb
 * @since 4.1.0
 * @see UnmatchedHostException
 */
@FunctionalInterface
public interface InetAddressMatcher {

	/**
	 * Test if the given socket address matches.
	 * @param address the socket address string to check
	 * @return {@code true} if the address matches, {@code false} otherwise
	 */
	default boolean matches(InetSocketAddress address) {
		Assert.notNull(address, "'address' must not be null");
		return matches(address.getAddress());
	}

	/**
	 * Whether the given address matches.
	 * @param address the address to check
	 * @return if the address matches
	 */
	boolean matches(InetAddress address);

	/**
	 * Adapts the given {@link Predicate} to an {@link InetAddressMatcher}.
	 * @param predicate the predicate to adapt
	 * @return a new {@link InetAddressMatcher} instance
	 */
	static InetAddressMatcher of(Predicate<@Nullable InetAddress> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return (address) -> {
			Assert.notNull(address, "'address' must not be null");
			return predicate.test(address);
		};
	}

}
