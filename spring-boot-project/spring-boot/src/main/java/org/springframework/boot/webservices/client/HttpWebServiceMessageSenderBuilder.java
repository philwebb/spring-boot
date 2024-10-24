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

package org.springframework.boot.webservices.client;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * {@link WebServiceMessageSender} builder that can detect a suitable HTTP library based
 * on the classpath.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class HttpWebServiceMessageSenderBuilder {

	// FIXME change to return new instance? Check scope on aut-con

	private ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

	private ClientHttpRequestFactorySettings requestFactorySettings = ClientHttpRequestFactorySettings.DEFAULTS;

	public void requestFactorySettings(ClientHttpRequestFactorySettings requestFactorySettings) {
		Assert.notNull(requestFactorySettings, "ClientHttpRequestFactorySettings must not be null");
		this.requestFactorySettings = requestFactorySettings;
	}

	/**
	 * Set the connection timeout.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setConnectTimeout(Duration connectTimeout) {
		this.requestFactorySettings = this.requestFactorySettings.withConnectTimeout(connectTimeout);
		return this;
	}

	/**
	 * Set the read timeout.
	 * @param readTimeout the read timeout
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setReadTimeout(Duration readTimeout) {
		this.requestFactorySettings = this.requestFactorySettings.withReadTimeout(readTimeout);
		return this;
	}

	/**
	 * Set an {@link SslBundle} that will be used to configure a secure connection.
	 * @param sslBundle the SSL bundle
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder sslBundle(SslBundle sslBundle) {
		this.requestFactorySettings = this.requestFactorySettings.withSslBundle(sslBundle);
		return this;
	}

	/**
	 * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * to create the HTTP-based {@link WebServiceMessageSender}.
	 * @param requestFactorySupplier the supplier for the request factory
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder requestFactory(
			Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier, "RequestFactorySupplier must not be null");
		this.requestFactoryBuilder = ClientHttpRequestFactoryBuilder.of(requestFactorySupplier);
		return this;
	}

	/**
	 * Set the {@code Function} of {@link ClientHttpRequestFactorySettings} to
	 * {@link ClientHttpRequestFactory} that should be called to create the HTTP-based
	 * {@link WebServiceMessageSender}.
	 * @param requestFactoryFunction the function for the request factory
	 * @return a new builder instance
	 * @since 3.0.0
	 */
	public HttpWebServiceMessageSenderBuilder requestFactory(
			Function<ClientHttpRequestFactorySettings, ClientHttpRequestFactory> requestFactoryFunction) {
		Assert.notNull(requestFactoryFunction, "RequestFactoryFunction must not be null");
		this.requestFactoryBuilder = requestFactoryFunction::apply;
		return this;
	}

	/**
	 * Set the {@link ClientHttpRequestFactoryBuilder} to use when creating the HTTP-based
	 * {@link WebServiceMessageSender}.
	 * @param requestFactoryBuilder the {@link ClientHttpRequestFactoryBuilder} to use
	 * @return this builder instance
	 * @since 3.4.0
	 */
	public HttpWebServiceMessageSenderBuilder requestFactoryBuilder(
			ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder) {
		Assert.notNull(requestFactoryBuilder, "ClientHttpRequestFactoryBuilder must not be null");
		this.requestFactoryBuilder = requestFactoryBuilder;
		return this;
	}

	/**
	 * Build the {@link WebServiceMessageSender} instance.
	 * @return the {@link WebServiceMessageSender} instance
	 */
	public WebServiceMessageSender build() {
		return new ClientHttpRequestMessageSender(getRequestFactory());
	}

	private ClientHttpRequestFactory getRequestFactory() {
		ClientHttpRequestFactoryBuilder<?> builder = (this.requestFactoryBuilder != null) ? this.requestFactoryBuilder
				: ClientHttpRequestFactoryBuilder.detect();
		return builder.build(this.requestFactorySettings);
	}

}
