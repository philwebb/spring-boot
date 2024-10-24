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
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#reactor()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public final class ReactorClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<ReactorClientHttpRequestFactory> {

	ReactorClientHttpRequestFactoryBuilder() {
		this(null);
	}

	private ReactorClientHttpRequestFactoryBuilder(List<Consumer<ReactorClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	@SafeVarargs
	public final ReactorClientHttpRequestFactoryBuilder withCustomizers(
			Consumer<ReactorClientHttpRequestFactory>... customizers) {
		return new ReactorClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
	}

	@Override
	protected ReactorClientHttpRequestFactory createClientHttpRequestFactory(
			ClientHttpRequestFactorySettings settings) {
		ReactorClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	private ReactorClientHttpRequestFactory createRequestFactory(SslBundle sslBundle) {
		if (sslBundle != null) {
			HttpClient httpClient = HttpClient.create()
				.secure((ThrowingConsumer.of((spec) -> configureSsl(spec, sslBundle))));
			return new ReactorClientHttpRequestFactory(httpClient);
		}
		return new ReactorClientHttpRequestFactory();
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
