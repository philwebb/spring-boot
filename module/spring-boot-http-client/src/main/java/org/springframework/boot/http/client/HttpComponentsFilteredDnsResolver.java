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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.DnsResolver;
import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * HTTP Components {@link DnsResolver} that filters using a
 * {@link InetAddressMatcher}.
 *
 * @author Phillip Webb
 * @param delegate the delegate resolver
 * @param matcher the inetAddress matcher
 */
record HttpComponentsFilteredDnsResolver(DnsResolver delegate,
		InetAddressMatcher matcher) implements DnsResolver {

	@Override
	public @Nullable InetAddress[] resolve(String host) throws UnknownHostException {
		InetAddress[] resolved = this.delegate.resolve(host);
		if (ObjectUtils.isEmpty(resolved)) {
			return resolved;
		}
		return MatchingAddresses.of(Arrays.stream(resolved))
			.toArray(this.matcher::matches, InetAddress[]::new)
			.orElseThrow(host, this.matcher);
	}

	@Override
	public List<InetSocketAddress> resolve(String host, int port) throws UnknownHostException {
		List<InetSocketAddress> resolved = this.delegate.resolve(host, port);
		if (resolved.isEmpty()) {
			return resolved;
		}
		return MatchingAddresses.of(resolved.stream()).toList(this.matcher::matches).orElseThrow(host, this.matcher);
	}

	@Override
	public String resolveCanonicalHostname(String host) throws UnknownHostException {
		return this.delegate.resolveCanonicalHostname(host);
	}

}
