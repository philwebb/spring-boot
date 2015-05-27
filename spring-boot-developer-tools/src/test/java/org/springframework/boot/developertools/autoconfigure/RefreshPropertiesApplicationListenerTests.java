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

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RefreshPropertiesApplicationListener}.
 *
 * @author Phillip Webb
 */
public class RefreshPropertiesApplicationListenerTests {

	private RefreshPropertiesApplicationListener listener = new RefreshPropertiesApplicationListener();

	@Test
	public void getOrder() throws Exception {
		assertThat(this.listener.getOrder(),
				greaterThan(ConfigFileApplicationListener.DEFAULT_ORDER));
	}

	@Test
	public void addsPropertiesOnApplicationEnvironmentPrepared() throws Exception {
		SpringApplication application = new SpringApplication();
		String[] args = {};
		ConfigurableEnvironment environment = new MockEnvironment();
		ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
				application, args, environment);
		this.listener.onApplicationEvent(event);
		assertThat(environment.getProperty("spring.thymeleaf.cache"), equalTo("false"));
	}

}
