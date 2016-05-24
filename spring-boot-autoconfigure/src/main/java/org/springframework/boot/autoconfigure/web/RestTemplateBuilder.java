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

import java.util.Arrays;
import java.util.List;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * A builder used to create {@link RestTemplate} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private ClientHttpRequestFactory requestFactory;

	private List<HttpMessageConverter<?>> httpMessageConverters;

	/**
	 * Use the specified {@link ClientHttpRequestFactory}.
	 * @param requestFactory the request factory to use
	 * @return this builder instance
	 * @see RestTemplate#setRequestFactory(ClientHttpRequestFactory)
	 */
	protected RestTemplateBuilder requestFactory(ClientHttpRequestFactory requestFactory) {
		this.requestFactory = requestFactory;
		return this;
	}

	/**
	 * Use the {@link HttpMessageConverter} instances defined by the specified
	 * {@link HttpMessageConverters}.
	 * @param httpMessageConverters the http message converters to use
	 * @return this builder instance
	 * @see #httpMessageConverters(HttpMessageConverter[])
	 */
	protected RestTemplateBuilder httpMessageConverters(HttpMessageConverters httpMessageConverters) {
		List<HttpMessageConverter<?>> converters = httpMessageConverters.getConverters();
		return httpMessageConverters(converters.toArray(new HttpMessageConverter[converters.size()]));
	}

	/**
	 * Use the specified {@link HttpMessageConverter} instances.
	 * @param httpMessageConverters the http message converters to use
	 * @return this builder instance
	 * @see RestTemplate#setMessageConverters(List)
	 */
	protected RestTemplateBuilder httpMessageConverters(HttpMessageConverter<?>... httpMessageConverters) {
		this.httpMessageConverters = Arrays.asList(httpMessageConverters);
		return this;
	}

	/**
	 * Build a new {@link RestTemplate} with the state of this instance.
	 * @return a rest template with this builder's settings
	 */
	public RestTemplate build() {
		RestTemplate restTemplate = createRestTemplate();
		configure(restTemplate);
		return restTemplate;
	}

	/**
	 * Configure the specified {@link RestTemplate} with the state of this instance.
	 * @param restTemplate the rest template to configure
	 */
	public void configure(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		if (this.requestFactory != null) {
			restTemplate.setRequestFactory(this.requestFactory);
		}
		if (this.httpMessageConverters != null) {
			restTemplate.setMessageConverters(this.httpMessageConverters);
		}
	}

	protected RestTemplate createRestTemplate() {
		return new RestTemplate();
	}

}
