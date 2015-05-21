/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.autoconfigure;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.developertools.restart.RestartInitializer;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.thymeleaf.templateresolver.TemplateResolver;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link LocalDeveloperToolsAutoConfiguration}.
 *
 * @author Phillip Webb
 */
public class LocalDeveloperToolsAutoConfigurationTests {

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	public void thymeleafCacheIsFalse() throws Exception {
		ConfigurableApplicationContext context = initializeAndRun();
		TemplateResolver resolver = context.getBean(TemplateResolver.class);
		resolver.initialize();
		assertThat(resolver.isCacheable(), equalTo(false));
	}

	private ConfigurableApplicationContext initializeAndRun() {
		Restarter.initialize(new String[0], false, new MockRestartInitializer(), false);
		SpringApplication application = new SpringApplication(Config.class);
		application.setDefaultProperties(getDefaultProperties());
		application.setWebEnvironment(false);
		ConfigurableApplicationContext context = application.run();
		return context;
	}

	private Map<String, Object> getDefaultProperties() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.check-template-location", "false");
		return properties;
	}

	@Configuration
	@Import({ LocalDeveloperToolsAutoConfiguration.class,
			ThymeleafAutoConfiguration.class })
	public static class Config {

	}

	private static class MockRestartInitializer implements RestartInitializer {

		@Override
		public URL[] getInitialUrls(Thread thread) {
			return new URL[] {};
		}

	}

}
