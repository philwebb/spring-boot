/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.IncludeExcludePropertyEndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(EndpointAutoConfiguration.class)
@EnableConfigurationProperties(WebEndpointProperties.class)
public class WebEndpointAutoConfiguration {

	private static final List<String> MEDIA_TYPES = Arrays
			.asList(ActuatorMediaType.V2_JSON, "application/json");

	private final ApplicationContext applicationContext;

	WebEndpointAutoConfiguration(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointPathResolver endpointPathResolver(Environment environment) {
		return new DefaultEndpointPathResolver(environment);
	}

	@Bean
	@ConditionalOnMissingBean
	public WebAnnotationEndpointDiscoverer webAnnotationEndpointDiscoverer(
			ParameterMapper parameterMapper, EndpointPathResolver endpointPathResolver,
			Collection<OperationMethodInvokerAdvisor> invokerAdvisors,
			Collection<EndpointFilter<WebOperation>> filters) {
		return new WebAnnotationEndpointDiscoverer(this.applicationContext,
				parameterMapper, endpointMediaTypes(), endpointPathResolver,
				invokerAdvisors, filters);
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointMediaTypes endpointMediaTypes() {
		return new EndpointMediaTypes(MEDIA_TYPES, MEDIA_TYPES);
	}

	@Bean
	@ConditionalOnMissingBean
	public EndpointPathProvider endpointPathProvider(
			EndpointDiscoverer<WebOperation> endpointDiscoverer,
			WebEndpointProperties webEndpointProperties) {
		return new DefaultEndpointPathProvider(endpointDiscoverer, webEndpointProperties);
	}

	@Bean
	public IncludeExcludePropertyEndpointFilter<WebOperation> webIncludeExcludePropertyEndpointFilter() {
		return new IncludeExcludePropertyEndpointFilter<>(
				WebAnnotationEndpointDiscoverer.class,
				this.applicationContext.getEnvironment(), "endpoints.web");
	}

}
