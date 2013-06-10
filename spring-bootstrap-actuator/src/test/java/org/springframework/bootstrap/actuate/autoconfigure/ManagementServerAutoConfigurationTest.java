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
package org.springframework.bootstrap.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.fail;

/**
 * Tests for {@link ManagementServerAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class ManagementServerAutoConfigurationTest {

	@Test
	public void onSamePort() {
		fail("Not yet implemented");
	}

	@Test
	public void onDifferentPort() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext applicationContext = new AnnotationConfigEmbeddedWebApplicationContext();
		applicationContext.register(DifferentPortConfig.class,
				ManagementServerAutoConfiguration.class);
		applicationContext.refresh();
		Thread.sleep(10000000000L);
		System.out.println("test");
	}

	@Configuration
	public static class DifferentPortConfig {

		@Bean
		public ServerProperties serverProperties() {
			return new ServerProperties();
		}

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(8081);
			return properties;
		}

	}

}
