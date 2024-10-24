/*
 * Copyright 2012-2024 the original author or authors.
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

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#httpComponents()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public final class HttpComponentsClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<HttpComponentsClientHttpRequestFactory> {

	HttpComponentsClientHttpRequestFactoryBuilder() {
		this(Collections.emptyList());
	}

	private HttpComponentsClientHttpRequestFactoryBuilder(
			List<Consumer<HttpComponentsClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizer(
			Consumer<HttpComponentsClientHttpRequestFactory> customizer) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizer));
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<HttpComponentsClientHttpRequestFactory>> customizers) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
	}

	@Override
	protected HttpComponentsClientHttpRequestFactory createClientHttpRequestFactory(
			ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = createHttpClient(settings.readTimeout(), settings.sslBundle());
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(factory::setConnectTimeout);
		return factory;
	}

	private HttpClient createHttpClient(Duration readTimeout, SslBundle sslBundle) {
		return HttpClientBuilder.create()
			.useSystemProperties()
			.setConnectionManager(createConnectionManager(readTimeout, sslBundle))
			.build();
	}

	private PoolingHttpClientConnectionManager createConnectionManager(Duration readTimeout, SslBundle sslBundle) {
		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
			.create();
		if (readTimeout != null) {
			connectionManagerBuilder.setDefaultSocketConfig(createSocketConfig(readTimeout));
		}
		if (sslBundle != null) {
			connectionManagerBuilder.setTlsSocketStrategy(createTlsSocketStrategy(sslBundle));
		}
		PoolingHttpClientConnectionManager connectionManager = connectionManagerBuilder.useSystemProperties().build();
		return connectionManager;
	}

	private DefaultClientTlsStrategy createTlsSocketStrategy(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		DefaultClientTlsStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(sslBundle.createSslContext(),
				options.getEnabledProtocols(), options.getCiphers(), null, new DefaultHostnameVerifier());
		return tlsSocketStrategy;
	}

	private SocketConfig createSocketConfig(Duration readTimeout) {
		return SocketConfig.custom().setSoTimeout((int) readTimeout.toMillis(), TimeUnit.MILLISECONDS).build();
	}

	static class Classes {

		static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.classic.HttpClients";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENTS, null);

	}

}
