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

import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Stephane Nicoll
 */
public class WebClientAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void buildDefaultRestTemplate() {
		load(HttpMessageConvertersAutoConfiguration.class, TestConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		HttpMessageConverters messageConverters = this.context.getBean(HttpMessageConverters.class);
		List<HttpMessageConverter<?>> converters = messageConverters.getConverters();
		assertThat(restTemplate.getMessageConverters()).containsExactly(
				converters.toArray(new HttpMessageConverter[converters.size()]));
		assertThat(restTemplate.getRequestFactory())
				.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	public void buildNoMessageConverters() {
		load(TestConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		RestTemplate defaultRestTemplate = new RestTemplate();
		assertThat(restTemplate.getMessageConverters().size()).isEqualTo(
				defaultRestTemplate.getMessageConverters().size());
	}

	@Test
	public void buildCustomMessageConverters() {
		load(CustomHttpMessageConverter.class,
				HttpMessageConvertersAutoConfiguration.class, TestConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(restTemplate.getMessageConverters()).has(
				new Condition<List<? extends HttpMessageConverter<?>>>() {
			@Override
			public boolean matches(List<? extends HttpMessageConverter<?>> value) {
				for (HttpMessageConverter<?> httpMessageConverter : value) {
					if (httpMessageConverter.getClass() == CustomHttpMessageConverter.class) {
						return true;
					}
				}
				return false;
			}
		});
	}

	@Test
	public void buildCustomBuilder() {
		load(TestConfig.class, CustomBuilderConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(restTemplate.getMessageConverters()).hasSize(1);
		assertThat(restTemplate.getMessageConverters().get(0)).isInstanceOf(CustomHttpMessageConverter.class);
	}


	public void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.register(WebClientAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class TestConfig {

		@Bean
		public RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}

	}

	@Configuration
	static class CustomBuilderConfig {

		@Bean
		public RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().httpMessageConverters(new CustomHttpMessageConverter());
		}

	}

	static class CustomHttpMessageConverter extends StringHttpMessageConverter {

	}

}
