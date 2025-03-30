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

import java.util.function.UnaryOperator;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Builder that can be used to create a Rector Netty {@link HttpClient}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public class ReactorHttpClientBuilder {

	private final UnaryOperator<HttpClient> customizer;

	public ReactorHttpClientBuilder() {
		this(UnaryOperator.identity());
	}

	private ReactorHttpClientBuilder(UnaryOperator<HttpClient> customizer) {
		this.customizer = customizer;
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that applies additional customization
	 * to the underlying {@link HttpClient}.
	 * @param customizer the customizer to apply
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new ReactorHttpClientBuilder((t) -> customizer.apply(this.customizer.apply(t)));
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param httpRedirects the HTTP follow redirects strategy
	 * @param sslBundle the SSL bundle to use
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(HttpRedirects httpRedirects, SslBundle sslBundle) {
		HttpClient httpClient = applyDefaults(HttpClient.create());
		httpClient = httpClient.followRedirect(followRedirects(httpRedirects));
		// FIXME httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 0);
		// httpClient.responseTimeout(readTimeout);
		if (sslBundle != null) {
			httpClient = httpClient.secure((ThrowingConsumer.of((spec) -> configureSsl(spec, sslBundle))));
		}
		return this.customizer.apply(httpClient);
	}

	private boolean followRedirects(HttpRedirects httpRedirects) {
		return switch (httpRedirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	HttpClient applyDefaults(HttpClient httpClient) {
		// Aligns with Spring Framework defaults
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

}
