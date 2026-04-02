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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.assertj.core.api.AbstractObjectAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions for an {@link InetAddressMatcher}.
 *
 * @author Phillip Webb
 */
class InetAddressMatcherAssert extends AbstractObjectAssert<InetAddressMatcherAssert, InetAddressMatcher> {

	InetAddressMatcherAssert(InetAddressMatcher actual) {
		super(actual, InetAddressMatcherAssert.class);
	}

	InetAddressMatcherAssert matches(String address) {
		return matches(asInetAddress(address));
	}

	InetAddressMatcherAssert matches(InetAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Matches address %s", address).isTrue();
		return this;
	}

	InetAddressMatcherAssert matches(InetSocketAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Matches socket address %s", address).isTrue();
		return this;
	}

	InetAddressMatcherAssert matchesString(String address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Matches string %s", address).isTrue();
		return this;
	}

	InetAddressMatcherAssert doesNotMatch(String address) {
		return doesNotMatch(asInetAddress(address));
	}

	InetAddressMatcherAssert doesNotMatch(InetAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Does not match address %s", address).isFalse();
		return this;
	}

	InetAddressMatcherAssert doesNotMatch(InetSocketAddress address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Does not match socket address %s", address).isFalse();
		return this;
	}

	InetAddressMatcherAssert doesNotMatchString(String address) {
		isNotNull();
		assertThat(this.actual.matches(address)).as("Does not match string %s", address).isFalse();
		return this;
	}

	private InetAddress asInetAddress(String address) {
		try {
			return InetAddress.getByName(address);
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
