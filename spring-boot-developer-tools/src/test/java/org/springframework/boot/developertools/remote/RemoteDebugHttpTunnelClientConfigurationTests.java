/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.boot.developertools.remote;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
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

/**
 *
 * @author Rob Winch
 * @since 1.3.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = RemoteDebugHttpTunnelClientConfigurationTests.BootApplication.class)
@WebIntegrationTest
public class RemoteDebugHttpTunnelClientConfigurationTests {
	@Value("${local.server.port}")
	int port;

	AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultSetup() {
		context = new AnnotationConfigApplicationContext();
		this.context.register(Config.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.developertools.remote.secret:supersecret");

		Map<String, Object> source = Collections.<String,Object>singletonMap("remoteUrl", "http://localhost:"+port);
		PropertySource<?> propertySource = new MapPropertySource("remoteUrl", source);
		context.getEnvironment().getPropertySources().addFirst(propertySource);
		this.context.refresh();


		String url = "http://localhost:" + port + "/hello";

		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("Hello World", entity.getBody());
	}

	@Test
	public void missingSecret() {
		context = new AnnotationConfigApplicationContext();
		this.context.register(Config.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context);

		Map<String, Object> source = Collections.<String,Object>singletonMap("remoteUrl", "http://localhost:"+port);
		PropertySource<?> propertySource = new MapPropertySource("remoteUrl", source);
		context.getEnvironment().getPropertySources().addFirst(propertySource);

		try {
			this.context.refresh();
			fail("Expected Exception");
		} catch(Exception e) {
			assertThat(e.getMessage(), containsString("The environment value spring.developertools.remote.secret is required to secure your connection."));
		}

	}

	@Configuration
	@Import(RemoteDebugHttpTunnelClientConfiguration.class)
	static class Config {

	}

	@Configuration
	@EnableAutoConfiguration(exclude=RemoteDebugHttpTunnelClientConfiguration.class)
	@RestController
	static class BootApplication {

		@RequestMapping
		public String hello() {
			return "Hello World";
		}
	}
}
