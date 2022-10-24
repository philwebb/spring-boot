/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * A supplier for {@link ClientHttpRequestFactory}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.1.0
 */
public interface ClientHttpRequestFactorySupplier {

	/**
	 * Returns a {@link ClientHttpRequestFactorySupplier} that will supply a
	 * {@link ClientHttpRequestFactorySupplier} for one of the known
	 * {@link ClientHttpRequestFactory} implementations.
	 * @return the supplier
	 * @since 3.0.0
	 */
	static ClientHttpRequestFactorySupplier fromKnownFactories() {
		return new KnownFactoriesClientHttpRequestFactorySupplier();
	}

	static ClientHttpRequestFactorySupplier forFactory(Class<? extends ClientHttpRequestFactory> factoryType) {
		return KnownFactoriesClientHttpRequestFactorySupplier.forFactoryType(factoryType);
	}

	ClientHttpRequestFactory get(Settings settings);

	static class Settings {

		private final Duration connectTimeout;

		private final Duration readTimeout;

		private final Boolean bufferRequestBody;

		public Settings() {
			this(null, null, null);
		}

		private Settings(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {
			this.connectTimeout = connectTimeout;
			this.readTimeout = readTimeout;
			this.bufferRequestBody = bufferRequestBody;
		}

		public Settings connectTimeout(Duration connectTimeout) {
			return new Settings(connectTimeout, this.readTimeout, this.bufferRequestBody);
		}

		public Settings readTimeout(Duration readTimeout) {
			return new Settings(this.connectTimeout, readTimeout, this.bufferRequestBody);
		}

		public Settings bufferRequestBody(Boolean bufferRequestBody) {
			return new Settings(this.connectTimeout, this.readTimeout, bufferRequestBody);
		}

		public Duration connectTimeout() {
			return this.connectTimeout;
		}

		public Duration readTimeout() {
			return this.readTimeout;
		}

		public Boolean bufferRequestBody() {
			return this.bufferRequestBody;
		}

	}

}
