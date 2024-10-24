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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jdk()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
public class JdkClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> {

	JdkClientHttpRequestFactoryBuilder() {
		this(null);
	}

	private JdkClientHttpRequestFactoryBuilder(List<Consumer<JdkClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	@SafeVarargs
	public final JdkClientHttpRequestFactoryBuilder withCustomizers(
			Consumer<JdkClientHttpRequestFactory>... customizers) {
		return new JdkClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
	}

	@Override
	protected JdkClientHttpRequestFactory createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = createHttpClient(settings.connectTimeout(), settings.sslBundle());
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::readTimeout).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	private HttpClient createHttpClient(Duration connectTimeout, SslBundle sslBundle) {
		HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
		if (connectTimeout != null) {
			httpClientBuilder.connectTimeout(connectTimeout);
		}
		if (sslBundle != null) {
			httpClientBuilder.sslContext(sslBundle.createSslContext());
		}
		return httpClientBuilder.build();
	}

	static class Classes {

		static final String HTTP_CLIENT = "java.net.http.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
