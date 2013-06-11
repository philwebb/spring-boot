/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.fixme;

import org.springframework.bootstrap.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.bootstrap.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.bootstrap.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.bootstrap.actuate.autoconfigure.TraceRepositoryAutoConfiguration;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for actuator.
 * 
 * @author Dave Syer
 */
@Configuration
@Import({ MetricRepositoryAutoConfiguration.class, ErrorConfiguration.class,
		TraceRepositoryAutoConfiguration.class, MetricFilterAutoConfiguration.class,
		AuditAutoConfiguration.class })
public class ActuatorAutoConfiguration {

	// ServerProperties has to be declared in a non-conditional bean, so that it gets
	// added to the context early enough

	// FIXME ^^ perhaps not any more

	@EnableConfigurationProperties
	public static class ActuatorServerPropertiesConfiguration {

		@ConditionalOnMissingBean(ManagementServerProperties.class)
		@Bean(name = "org.springframework.bootstrap.actuate.properties.ManagementServerProperties")
		public ManagementServerProperties managementServerProperties() {
			return new ManagementServerProperties();
		}
		//
		// @Bean
		// @ConditionalOnMissingBean(EndpointsProperties.class)
		// public EndpointsProperties endpointsProperties() {
		// return new EndpointsProperties();
		// }

	}

}
