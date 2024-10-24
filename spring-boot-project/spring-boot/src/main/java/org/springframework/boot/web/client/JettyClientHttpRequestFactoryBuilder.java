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

package org.springframework.boot.web.client;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jetty()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public final class JettyClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<JettyClientHttpRequestFactory> {

	JettyClientHttpRequestFactoryBuilder() {
		this(null);
	}

	private JettyClientHttpRequestFactoryBuilder(List<Consumer<JettyClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	@SafeVarargs
	public final JettyClientHttpRequestFactoryBuilder withCustomizers(
			Consumer<JettyClientHttpRequestFactory>... customizers) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
	}

	@Override
	protected JettyClientHttpRequestFactory createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings) {
		JettyClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	private static JettyClientHttpRequestFactory createRequestFactory(SslBundle sslBundle) {
		if (sslBundle != null) {
			SSLContext sslContext = sslBundle.createSslContext();
			SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
			sslContextFactory.setSslContext(sslContext);
			ClientConnector connector = new ClientConnector();
			connector.setSslContextFactory(sslContextFactory);
			HttpClient httpClient = new HttpClient(new HttpClientTransportDynamic(connector));
			return new JettyClientHttpRequestFactory(httpClient);
		}
		return new JettyClientHttpRequestFactory();
	}

	static class Classes {

		static final String HTTP_CLIENT = "org.eclipse.jetty.client.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
