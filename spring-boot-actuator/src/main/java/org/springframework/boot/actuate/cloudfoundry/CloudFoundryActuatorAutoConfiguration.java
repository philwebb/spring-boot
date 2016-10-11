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

package org.springframework.boot.actuate.cloudfoundry;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.actuate.endpoint.mvc.NamedMvcEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Configuration to to expose {@link Endpoint} instances for Cloud Foundry specific path.
 *
 * @author Madhura Bhave
 * @since 1.5.0
 */
@Configuration
@ConditionalOnProperty(prefix = "management.cloudfoundry", name = "enabled", matchIfMissing = true)
@ConditionalOnBean(MvcEndpoints.class)
@AutoConfigureAfter(EndpointWebMvcAutoConfiguration.class)
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundryActuatorAutoConfiguration {

	@Bean
	public CloudFoundryEndpointHandlerMapping cloudFoundryEndpointHandlerMapping(
			MvcEndpoints mvcEndpoints) {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin(CorsConfiguration.ALL);
		Set<NamedMvcEndpoint> endpoints = new LinkedHashSet<NamedMvcEndpoint>(
				mvcEndpoints.getEndpoints(NamedMvcEndpoint.class));
		CloudFoundryEndpointHandlerMapping mapping = new CloudFoundryEndpointHandlerMapping(
				endpoints, corsConfiguration);
		mapping.setPrefix("/cloudfoundryapplication");
		return mapping;
	}

}
