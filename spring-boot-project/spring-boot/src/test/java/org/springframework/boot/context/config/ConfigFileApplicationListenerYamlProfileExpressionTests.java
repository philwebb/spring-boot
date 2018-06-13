/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.config;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigFileApplicationListener} handling of profiles expressions in
 * yaml configuration files.
 *
 * @author Phillip Webb
 */
public class ConfigFileApplicationListenerYamlProfileExpressionTests {

	private ConfigurableApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void yamlProfileNegationWithActiveProfile() {
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		String configName = "--spring.config.name=profileexpression";
		this.context = application.run(configName, "--spring.profiles.active=A,B");
		Environment environment = this.context.getEnvironment();
		assertThat(environment.containsProperty("default")).isTrue();
		assertThat(environment.containsProperty("aandb")).isTrue();
		assertThat(environment.containsProperty("aandc")).isFalse();
		assertThat(environment.containsProperty("aorc")).isTrue();
		assertThat(environment.containsProperty("aandcoraandb")).isTrue();
	}

	@Configuration
	public static class Config {

	}

}
