/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.autoconfigure.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.devservices.dockercompose.interop.DockerComposeOrigin;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
@DisabledIfDockerUnavailable
public abstract class AbstractIntegrationTests {

	@TempDir
	static Path tempDir;

	private static List<Runnable> shutdownHandler;

	@BeforeAll
	static void beforeAll() {
		shutdownHandler = new ArrayList<>();
	}

	@AfterAll
	static void afterAll() {
		for (Runnable runnable : shutdownHandler) {
			runnable.run();
		}
	}

	@BeforeEach
	void setUp() throws IOException {
		createComposeYaml();
	}

	protected abstract InputStream getComposeContent();

	protected final <T extends ConnectionDetails> T runProvider(Class<T> serviceConnectionClass) {
		return runProvider(new MockEnvironment(), serviceConnectionClass);
	}

	protected final <T extends ConnectionDetails> T runProvider(MockEnvironment environment,
			Class<T> serviceConnectionClass) {
		environment.setProperty("spring.dev-services.docker-compose.stop-mode", "down");
		DockerComposeListener dockerComposeListener = createProvider(environment);
		GenericApplicationContext context = new GenericApplicationContext();
		context.setEnvironment(environment);
		dockerComposeListener
			.onApplicationEvent(new ApplicationPreparedEvent(new SpringApplication(), new String[0], context));
		context.refresh();
		T serviceConnection = context.getBean(serviceConnectionClass);
		assertThat(serviceConnection.getOrigin()).isInstanceOf(DockerComposeOrigin.class);
		return serviceConnection;
	}

	private DockerComposeListener createProvider(Environment environment) {
		return new DockerComposeListener(getClass().getClassLoader(), environment, null, null, null, tempDir,
				new SpringApplicationShutdownHandlers() {

					@Override
					public void add(Runnable action) {
						shutdownHandler.add(action);
					}

					@Override
					public void remove(Runnable action) {
					}

				});
	}

	private void createComposeYaml() throws IOException {
		try (InputStream stream = getComposeContent()) {
			byte[] content = stream.readAllBytes();
			Files.write(tempDir.resolve("compose.yaml"), content);
		}
	}

}
