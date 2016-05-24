/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web;

import org.apache.http.client.HttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Actual RestTemplate configurations imported by {@link WebClientAutoConfiguration}.

 * @author Stephane Nicoll
 */
abstract class RestTemplateConfiguration {

	private final ObjectProvider<HttpMessageConverters> messageConvertersProvider;

	protected RestTemplateConfiguration(ObjectProvider<HttpMessageConverters> messageConvertersProvider) {
		this.messageConvertersProvider = messageConvertersProvider;
	}

	protected RestTemplateBuilder createRestTemplateBuilder() {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		HttpMessageConverters messageConverters = this.messageConvertersProvider.getIfUnique();
		if (messageConverters != null) {
			builder.httpMessageConverters(messageConverters);
		}
		return builder;
	}

	@ConditionalOnClass(HttpClient.class)
	public static class HttpComponents extends RestTemplateConfiguration {

		HttpComponents(
				ObjectProvider<HttpMessageConverters> messageConvertersProvider) {
			super(messageConvertersProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		public RestTemplateBuilder restTemplateBuilder() {
			return createRestTemplateBuilder()
					.requestFactory(new HttpComponentsClientHttpRequestFactory());
		}

	}

	public static class DefaultRequestFactory extends RestTemplateConfiguration {

		DefaultRequestFactory(
				ObjectProvider<HttpMessageConverters> messageConvertersProvider) {
			super(messageConvertersProvider);
		}

		@Bean
		@ConditionalOnMissingBean
		public RestTemplateBuilder restTemplateBuilder() {
			return createRestTemplateBuilder()
					.requestFactory(new HttpComponentsClientHttpRequestFactory());
		}

	}

}
