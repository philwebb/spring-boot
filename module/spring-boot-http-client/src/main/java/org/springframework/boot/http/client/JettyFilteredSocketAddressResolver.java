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

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

/**
 * Jetty {@link SocketAddressResolver} that filters using a {@link InetAddressMatcher}.
 *
 * @author Phillip Webb
 * @param delegate the delegate resolver
 * @param matcher the inetAddress matcher
 */
record JettyFilteredSocketAddressResolver(SocketAddressResolver delegate,
		InetAddressMatcher matcher) implements SocketAddressResolver {

	@Override
	public void resolve(String host, int port, Map<String, Object> context, Promise<List<InetSocketAddress>> promise) {
		this.delegate.resolve(host, port, context, new FilteredPromise(host, promise, this.matcher));
	}

	record FilteredPromise(String host, Promise<List<InetSocketAddress>> delegate,
			InetAddressMatcher matcher) implements Promise<List<InetSocketAddress>> {

		@Override
		public void succeeded(List<InetSocketAddress> result) {
			try {
				this.delegate.succeeded(MatchingAddresses.of(result.stream())
					.toList(this.matcher::matches)
					.orElseThrow(this.host, this.matcher));
			}
			catch (UnmatchedHostException ex) {
				failed(ex);
			}
		}

		@Override
		public void failed(Throwable ex) {
			this.delegate.failed(ex);
		}

	}
}
