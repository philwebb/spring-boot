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

package smoketest.actuator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;

@SpringBootApplication(proxyBeanMethods = false)
@ConfigurationPropertiesScan
public class SampleActuatorApplication {

	@Bean
	@SuppressWarnings("removal")
	public TanzuWebMvcEndpointHandlerMapping tanzuEndpointHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
			org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier servletEndpointsSupplier,
			ControllerEndpointsSupplier controllerEndpointsSupplier, EndpointMediaTypes endpointMediaTypes,
			ObjectProvider<CorsEndpointProperties> corsPropertiesProvider, WebEndpointProperties webEndpointProperties,
			Environment environment, ApplicationContext applicationContext, ParameterValueMapper parameterMapper) {
		CorsEndpointProperties corsProperties = corsPropertiesProvider.getIfAvailable();
		CorsConfiguration corsConfiguration = (corsProperties != null) ? corsProperties.toCorsConfiguration() : null;
		WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(applicationContext, parameterMapper,
				endpointMediaTypes, null, Collections.emptyList(),
				Collections.singletonList(new IncludeExcludeEndpointFilter<>(ExposableWebEndpoint.class, environment,
						"management.endpoints.tanzu.exposure", TanzuEndpointExposerFactory.DEFAULT_INCLUDES)));
		Collection<ExposableWebEndpoint> webEndpoints = discoverer.getEndpoints();
		List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
		allEndpoints.addAll(webEndpoints);
		allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
		allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
		return new TanzuWebMvcEndpointHandlerMapping(webEndpoints, endpointMediaTypes, corsConfiguration,
				new EndpointLinksResolver(allEndpoints, "/tanzu"));
	}

	@Bean
	public HealthIndicator helloHealthIndicator() {
		return createHealthIndicator("world");
	}

	@Bean
	public HealthContributor compositeHelloHealthContributor() {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("spring", createNestedHealthContributor("spring"));
		map.put("boot", createNestedHealthContributor("boot"));
		return CompositeHealthContributor.fromMap(map);
	}

	private HealthContributor createNestedHealthContributor(String name) {
		Map<String, HealthContributor> map = new LinkedHashMap<>();
		map.put("a", createHealthIndicator(name + "-a"));
		map.put("b", createHealthIndicator(name + "-b"));
		map.put("c", createHealthIndicator(name + "-c"));
		return CompositeHealthContributor.fromMap(map);
	}

	private HealthIndicator createHealthIndicator(String value) {
		return () -> Health.up().withDetail("hello", value).build();
	}

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SampleActuatorApplication.class);
		application.setApplicationStartup(new BufferingApplicationStartup(1024));
		application.run(args);
	}

}
