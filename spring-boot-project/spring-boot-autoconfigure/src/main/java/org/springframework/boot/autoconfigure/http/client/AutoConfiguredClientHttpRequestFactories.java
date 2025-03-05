/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.http.client;

import java.time.Duration;
import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.http.client.HttpClientProperties.Ssl;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.StringUtils;

/**
 * Helper class that can be used to create {@link ClientHttpRequestFactoryBuilder} and
 * {@link ClientHttpRequestFactorySettings} from auto-configuration and configuration
 * properties.
 *
 * @author Phillip Webb
 */
public class AutoConfiguredClientHttpRequestFactories {

	private final DefaultHttpClientProperties defaultHttpClientProperties;

	private final ObjectProvider<SslBundles> sslBundles;

	public AutoConfiguredClientHttpRequestFactories(DefaultHttpClientProperties defaultHttpClientProperties,
			ObjectProvider<SslBundles> sslBundles) {
		this.defaultHttpClientProperties = defaultHttpClientProperties;
		this.sslBundles = sslBundles;
	}

	public ClientHttpRequestFactoryBuilder<?> builder() {
		return builder(null);
	}

	public ClientHttpRequestFactoryBuilder<?> builder(HttpClientProperties properties) {
		HttpClientProperties.Factory factory = getProperty(properties, HttpClientProperties::getFactory);
		return (factory != null) ? factory.builder() : ClientHttpRequestFactoryBuilder.detect();
	}

	public ClientHttpRequestFactorySettings settings() {
		return settings(null);
	}

	public ClientHttpRequestFactorySettings settings(HttpClientProperties properties) {
		Redirects redirects = getProperty(properties, HttpClientProperties::getRedirects);
		Duration connectTimeout = getProperty(properties, HttpClientProperties::getConnectTimeout);
		Duration readTimeout = getProperty(properties, HttpClientProperties::getReadTimeout);
		String sslBundleName = getProperty(properties, HttpClientProperties::getSsl, Ssl::getBundle);
		SslBundle sslBundle = (StringUtils.hasLength(sslBundleName))
				? this.sslBundles.getObject().getBundle(sslBundleName) : null;
		return new ClientHttpRequestFactorySettings(redirects, connectTimeout, readTimeout, sslBundle);
	}

	private <T> T getProperty(HttpClientProperties properties, Function<HttpClientProperties, T> propertyProvider) {
		return getProperty(properties, Function.identity(), propertyProvider);
	}

	private <P, T> T getProperty(HttpClientProperties httpClientProperties, Function<HttpClientProperties, P> accessor,
			Function<P, T> getter) {
		P fallbackProperties = accessor.apply(this.defaultHttpClientProperties);
		P properties = apply(accessor, httpClientProperties);
		T value = apply(getter, properties);
		return (value != null) ? value : apply(getter, fallbackProperties);
	}

	private <T, R> R apply(Function<T, R> function, T t) {
		return (t != null) ? function.apply(t) : null;
	}

}
