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

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface used for {@link InetAddress}-based matching.
 * <p>
 * Used in {@link HttpClientSettings} to configure imperative or reactive HTTP clients to
 * restrict IP addresses that can be called. This interface is designed to allow
 * Server-Side Request Forgery (SSRF) mitigation features to be integrated with HTTP
 * clients build by Spring Boot.
 * <p>
 * When Spring Security is available on the classpath,
 * {@code org.springframework.security.web.util.matcher.InetAddressMatchers} may be used
 * to create matches, for example: <pre class="code">
 * InetAddressMatcher securityMatcher = InetAddressMatchers.matchInternal().build();
 * HttpClientInetAddressMatcher clientMatcher = securityMatcher::matches;
 * </pre>
 *
 * @author Phillip Webb
 * @since 4.1.0
 * @see UnmatchedHostException
 */
@FunctionalInterface
public interface HttpClientInetAddressMatcher {

	default boolean matches(@Nullable InetSocketAddress socketAddress) {
		InetAddress address = (socketAddress != null) ? socketAddress.getAddress() : null;
		return matches(address);
	}

	/**
	 * Return {@code true} if the given {@code address} matches and can be used by the
	 * HTTP client.
	 * @param address the address to check
	 * @return if the address matches
	 */
	boolean matches(@Nullable InetAddress address);

}
