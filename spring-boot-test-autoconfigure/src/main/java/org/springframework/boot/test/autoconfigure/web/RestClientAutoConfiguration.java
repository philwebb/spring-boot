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

package org.springframework.boot.test.autoconfigure.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.RestTemplateBuilder;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Stephane Nicoll
 */
@Configuration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
class RestClientAutoConfiguration {

	private final ObjectProvider<RestTemplateBuilder> restTemplateBuilderProvider;

	RestClientAutoConfiguration(ObjectProvider<RestTemplateBuilder> restTemplateBuilderProvider) {
		this.restTemplateBuilderProvider = restTemplateBuilderProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public RestTemplate restTemplate() {
		RestTemplateBuilder builder = this.restTemplateBuilderProvider.getIfAvailable();
		if (builder != null) {
			return builder.build();
		}
		return new RestTemplate();
	}

	@Bean
	@ConditionalOnMissingBean
	public MockRestServiceServer.MockRestServiceServerBuilder mockRestServiceServerBuilder(
			RestTemplate restTemplate) {
		return MockRestServiceServer.bindTo(restTemplate);
	}

}
