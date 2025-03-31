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

package org.springframework.boot.http.client;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.HttpClientSettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link HttpAsyncClient}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public class HttpComponentsHttpAsyncClientBuilder {

	private final Consumer<HttpAsyncClientBuilder> customizer;

	private final Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer;

	private final Consumer<ConnectionConfig.Builder> connectionConfigCustomizer;

	private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

	private final Function<SslBundle, TlsStrategy> tlsStrategyFactory;

	HttpComponentsHttpAsyncClientBuilder() {
		this(Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(),
				HttpComponentsHttpAsyncClientBuilder::createTlsSocketStrategy);
	}

	// FIXME pull up or extract

	private static TlsStrategy createTlsSocketStrategy(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		return new DefaultClientTlsStrategy(sslBundle.createSslContext(), options.getEnabledProtocols(),
				options.getCiphers(), null, new DefaultHostnameVerifier());
	}

	private HttpComponentsHttpAsyncClientBuilder(Consumer<HttpAsyncClientBuilder> customizer,
			Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer,
			Consumer<ConnectionConfig.Builder> connectionConfigCustomizer,
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
			Function<SslBundle, TlsStrategy> tlsStrategyFactory) {
		this.customizer = customizer;
		this.connectionManagerCustomizer = connectionManagerCustomizer;
		this.connectionConfigCustomizer = connectionConfigCustomizer;
		this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
		this.tlsStrategyFactory = tlsStrategyFactory;
	}

	/**
	 * Build a new {@link HttpAsyncClient} instance.
	 * @return a new {@link CloseableHttpAsyncClient} instance
	 */
	public CloseableHttpAsyncClient build() {
		return build(HttpClientSettings.defaults());
	}

	/**
	 * Build a new {@link HttpAsyncClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link CloseableHttpAsyncClient} instance
	 */
	public CloseableHttpAsyncClient build(HttpClientSettings settings) {
		Assert.notNull(settings, "'settings' must not be null");
		HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create()
			.useSystemProperties()
			.setRedirectStrategy(asRedirectStrategy(settings.redirects()))
			.setConnectionManager(createConnectionManager(settings))
			.setDefaultRequestConfig(createDefaultRequestConfig());
		this.customizer.accept(builder);
		return builder.build();
	}

	private PoolingAsyncClientConnectionManager createConnectionManager(HttpClientSettings settings) {
		PoolingAsyncClientConnectionManagerBuilder builder = PoolingAsyncClientConnectionManagerBuilder.create()
			.useSystemProperties();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		builder.setDefaultConnectionConfig(createConnectionConfig(settings));
		map.from(settings::sslBundle).as(this.tlsStrategyFactory).to(builder::setTlsStrategy);
		this.connectionManagerCustomizer.accept(builder);
		return builder.build();
	}

	private ConnectionConfig createConnectionConfig(HttpClientSettings settings) {
		ConnectionConfig.Builder builder = ConnectionConfig.custom();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::readTimeout)
			.asInt(Duration::toMillis)
			.to((timeout) -> builder.setSocketTimeout(timeout, TimeUnit.MILLISECONDS));
		this.connectionConfigCustomizer.accept(builder);
		return builder.build();
	}

	private RedirectStrategy asRedirectStrategy(Redirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> DefaultRedirectStrategy.INSTANCE;
			case DONT_FOLLOW -> NoFollowRedirectStrategy.INSTANCE;
		};
	}

	private RequestConfig createDefaultRequestConfig() {
		RequestConfig.Builder builder = RequestConfig.custom();
		this.defaultRequestConfigCustomizer.accept(builder);
		return builder.build();
	}

	/**
	 * {@link RedirectStrategy} that never follows redirects.
	 */
	private static final class NoFollowRedirectStrategy implements RedirectStrategy {

		private static final RedirectStrategy INSTANCE = new NoFollowRedirectStrategy();

		private NoFollowRedirectStrategy() {
		}

		@Override
		public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
			return false;
		}

		@Override
		public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) {
			return null;
		}

	}

}
