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

package org.springframework.boot.http.client.reactive;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ReactorClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Builder for {@link ClientHttpConnectorBuilder#reactor()}.
 *
 * @author Phillip Webb
 */
public class ReactorClientHttpConnectorBuilder
		extends AbstractClientHttpRequestFactoryBuilder<ReactorClientHttpConnector> {

	private final UnaryOperator<HttpClient> httpClientCustomizer;

	ReactorClientHttpConnectorBuilder() {
		this(null, UnaryOperator.identity());
	}

	private ReactorClientHttpConnectorBuilder(List<Consumer<ReactorClientHttpConnector>> customizers,
			UnaryOperator<HttpClient> httpClientCustomizer) {
		super(customizers);
		this.httpClientCustomizer = httpClientCustomizer;
	}

	@Override
	public ReactorClientHttpConnectorBuilder withCustomizer(Consumer<ReactorClientHttpConnector> customizer) {
		return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientCustomizer);
	}

	@Override
	public ReactorClientHttpConnectorBuilder withCustomizers(
			Collection<Consumer<ReactorClientHttpConnector>> customizers) {
		return new ReactorClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientCustomizer);
	}

	/**
	 * Return a new {@link ReactorClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link ReactorClientHttpRequestFactoryBuilder} instance
	 */
	public ReactorClientHttpConnectorBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new ReactorClientHttpConnectorBuilder(getCustomizers(),
				(t) -> httpClientCustomizer.apply(this.httpClientCustomizer.apply(t)));
	}

	@Override
	protected ReactorClientHttpConnector createClientHttpRequestFactory(ClientHttpConnectorSettings settings) {
		ReactorClientHttpConnector connector = createConnector(settings);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		// map.from(settings::connectTimeout).asInt(Duration::toMillis).to(connector::setConnectTimeout);
		// map.from(settings::readTimeout).asInt(Duration::toMillis).to(connector::setReadTimeout);
		return connector;
	}

	// FIXME extract common code or copy/paste

	private ReactorClientHttpConnector createConnector(ClientHttpConnectorSettings settings) {
		HttpClient httpClient = applyDefaults(HttpClient.create());
		httpClient = httpClient.followRedirect(followRedirects(settings.redirects()));
		if (settings.sslBundle() != null) {
			httpClient = httpClient.secure((ThrowingConsumer.of((spec) -> configureSsl(spec, settings.sslBundle()))));
		}
		httpClient = this.httpClientCustomizer.apply(httpClient);
		return new ReactorClientHttpConnector(httpClient);
	}

	private boolean followRedirects(Redirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	HttpClient applyDefaults(HttpClient httpClient) {
		// Aligns with ReactorClientHttpConnector defaults
		return httpClient.compress(true);
	}

	private void configureSsl(SslContextSpec spec, SslBundle sslBundle) throws SSLException {
		SslOptions options = sslBundle.getOptions();
		SslManagerBundle managers = sslBundle.getManagers();
		SslContextBuilder builder = SslContextBuilder.forClient()
			.keyManager(managers.getKeyManagerFactory())
			.trustManager(managers.getTrustManagerFactory())
			.ciphers(SslOptions.asSet(options.getCiphers()))
			.protocols(options.getEnabledProtocols());
		spec.sslContext(builder.build());
	}

	static class Classes {

		static final String HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
