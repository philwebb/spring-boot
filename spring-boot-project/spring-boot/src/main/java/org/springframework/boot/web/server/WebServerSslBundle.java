/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.server;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslManagers;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStores;

/**
 * @author Phillip Webb
 */
public class WebServerSslBundle implements SslBundle {

	private final SslKeyReference key;

	private final SslStores stores;

	private final SslManagers managers;

	private final SslOptions options;

	private WebServerSslBundle(Ssl ssl, SslStoreProvider sslStoreProvider) {
		String keyPassword = (sslStoreProvider != null) ? sslStoreProvider.getKeyPassword() : ssl.getKeyPassword();
		this.key = SslKeyReference.of(ssl.getKeyAlias(), keyPassword);
		this.stores = null;
		this.managers = SslManagers.from(this.stores);
		Set<String> enabledProtocols = (ssl.getEnabledProtocols() != null) ? Set.of(ssl.getEnabledProtocols())
				: Collections.emptySet();
		Set<String> ciphers = (ssl.getCiphers() != null) ? Set.of(ssl.getCiphers()) : Collections.emptySet();
		this.options = new SslOptions() {

			@Override
			public Set<String> getEnabledProtocols() {
				return enabledProtocols;
			}

			@Override
			public Set<String> getCiphers() {
				return ciphers;
			}
		};
	}

	@Override
	public SslKeyReference getKey() {
		return this.key;
	}

	@Override
	public SslStores getStores() {
		return this.stores;
	}

	@Override
	public SslManagers getManagers() {
		return this.managers;
	}

	@Override
	public SslOptions getOptions() {
		return this.options;
	}

}
