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

package org.springframework.boot.developertools.remote.client;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.developertools.restart.MockRestarter;
import org.springframework.boot.developertools.restart.RestartScopeInitializer;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link RemoteClientConfiguration}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RemoteClientConfigurationTests.BootApplication.class)
@WebIntegrationTest
public class RemoteClientConfigurationTests {

	// FIXME refactor this test

	@Rule
	public MockRestarter mockRestarter = new MockRestarter();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Value("${local.server.port}")
	private int port;

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultSetup() {
		setupContext("spring.developertools.remote.secret:supersecret");
		this.context.refresh();
		String url = "http://localhost:" + this.port + "/hello";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(entity.getStatusCode(), equalTo(HttpStatus.OK));
		assertThat(entity.getBody(), equalTo("Hello World"));
	}

	@Test
	public void missingSecret() {
		setupContext();
		this.thrown.expect(BeanCreationException.class);
		this.thrown.expectMessage("The environment value "
				+ "'spring.developertools.remote.secret' "
				+ "is required to secure your connection.");
		this.context.refresh();
	}

	private void setupContext(String... pairs) {
		this.context = new AnnotationConfigApplicationContext();
		new RestartScopeInitializer().initialize(this.context);
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, pairs);
		Map<String, Object> source = Collections.<String, Object> singletonMap(
				"remoteUrl", "http://localhost:" + this.port);
		PropertySource<?> propertySource = new MapPropertySource("remoteUrl", source);
		this.context.getEnvironment().getPropertySources().addFirst(propertySource);
	}

	@Configuration
	@Import(RemoteClientConfiguration.class)
	static class Config {

	}

	@Configuration
	@EnableAutoConfiguration(exclude = { RemoteClientConfiguration.class,
			ThymeleafAutoConfiguration.class })
	@RestController
	static class BootApplication {

		@RequestMapping
		public String hello() {
			return "Hello World";
		}

	}

}
