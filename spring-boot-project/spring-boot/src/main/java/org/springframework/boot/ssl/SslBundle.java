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

package org.springframework.boot.ssl;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.springframework.util.StringUtils;

/**
 * A bundle of trust material that can be used to establish an SSL connection.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public interface SslBundle {

	/**
	 * The default protocol to use.
	 */
	String DEFAULT_PROTOCOL = "TLS";

	/**
	 * Return the protocol to use when establishing the connection. Values should be
	 * supported by {@link SSLContext#getInstance(String)} Defaults to
	 * {@value #DEFAULT_PROTOCOL}.
	 * @return the SSL protocol
	 * @see SSLContext#getInstance(String)
	 */
	default String getProtocol() {
		return DEFAULT_PROTOCOL;
	}

	/**
	 * Return a reference the key that should be used for this bundle or
	 * {@link SslKeyReference#NONE}.
	 * @return a reference to the SSL key that should be used
	 */
	SslKeyReference getKey();

	/**
	 * Return the {@link SslStoreBundle} that can be used to access this bundle's key and
	 * trust stores.
	 * @return the {@code SslKeyStores} instance for this bundle
	 */
	SslStoreBundle getStores();

	/**
	 * Return the {@link SslManagerBundle} that can be used to access this bundle's
	 * {@link KeyManager key} and {@link TrustManager trust} managers.
	 * @return the {@code SslManagers} instance for this bundle
	 */
	SslManagerBundle getManagers();

	/**
	 * Return {@link SslOptions} that should be applied when establishing the SSL
	 * connection.
	 * @return the options that should be applied
	 */
	SslOptions getOptions();

	/**
	 * Create a new {@link SSLContext} for this bundle.
	 * @return a new {@link SSLContext} instance
	 */
	default SSLContext createSslContext() {
		return getManagers().createSslContext(getProtocol());
	}

	/**
	 * Create a new {@link SslBundle} instance.
	 * @param stores the stores or {@code null}
	 * @return a new {@link SslBundle} instance
	 */
	static SslBundle of(SslStoreBundle stores) {
		return of(null, stores, null);
	}

	/**
	 * Create a new {@link SslBundle} instance.
	 * @param key the key or {@code null}
	 * @param stores the stores or {@code null}
	 * @return a new {@link SslBundle} instance
	 */
	static SslBundle of(String key, SslStoreBundle stores) {
		return of(SslKeyReference.of(key), stores, null);
	}

	/**
	 * Create a new {@link SslBundle} instance.
	 * @param key the key or {@code null}
	 * @param stores the stores or {@code null}
	 * @return a new {@link SslBundle} instance
	 */
	static SslBundle of(SslKeyReference key, SslStoreBundle stores) {
		return of(key, stores, null);
	}

	/**
	 * Create a new {@link SslBundle} instance.
	 * @param key the key or {@code null}
	 * @param stores the stores or {@code null}
	 * @param options the options or {@code null}
	 * @return a new {@link SslBundle} instance
	 */
	static SslBundle of(SslKeyReference key, SslStoreBundle stores, SslOptions options) {
		return of(null, key, stores, options);
	}

	/**
	 * Create a new {@link SslBundle} instance.
	 * @param protocol the protocol or {@code null}
	 * @param key the key or {@code null}
	 * @param stores the stores or {@code null}
	 * @param options the options or {@code null}
	 * @return a new {@link SslBundle} instance
	 */
	static SslBundle of(String protocol, SslKeyReference key, SslStoreBundle stores, SslOptions options) {
		SslManagerBundle managers = SslManagerBundle.from(stores, key);
		return new SslBundle() {

			@Override
			public String getProtocol() {
				return (!StringUtils.hasText(protocol)) ? DEFAULT_PROTOCOL : protocol;
			}

			@Override
			public SslKeyReference getKey() {
				return (key != null) ? key : SslKeyReference.NONE;
			}

			@Override
			public SslStoreBundle getStores() {
				return (stores != null) ? stores : SslStoreBundle.NONE;
			}

			@Override
			public SslOptions getOptions() {
				return (options != null) ? options : SslOptions.NONE;
			}

			@Override
			public SslManagerBundle getManagers() {
				return managers;
			}

		};
	}

}
