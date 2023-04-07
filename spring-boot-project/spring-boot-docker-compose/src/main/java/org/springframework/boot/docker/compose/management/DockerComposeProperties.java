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

package org.springframework.boot.docker.compose.management;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * Configuration properties for the 'docker compose'.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@ConfigurationProperties(DockerComposeProperties.NAME)
class DockerComposeProperties {

	static final String NAME = "spring.docker.compose";

	/**
	 * Whether docker compose support is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Path to a specific docker compose configuration file.
	 */
	private File file;

	/**
	 * Docker compose lifecycle management.
	 */
	private LifecycleManagement lifecycleManagement = LifecycleManagement.START_AND_STOP;

	/**
	 * Hostname or IP of the machine where the docker containers are started.
	 */
	private String host;

	/**
	 * Start configuration.
	 */
	private final Startup startup = new Startup();

	/**
	 * Stop configuration.
	 */
	private final Shutdown shutdown = new Shutdown();

	/**
	 * Profiles configuration.
	 */
	private final Profiles profiles = new Profiles();

	private final Skip skip = new Skip();

	boolean isEnabled() {
		return this.enabled;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	File getFile() {
		return this.file;
	}

	void setFile(File file) {
		this.file = file;
	}

	LifecycleManagement getLifecycleManagement() {
		return this.lifecycleManagement;
	}

	void setLifecycleManagement(LifecycleManagement lifecycleManagement) {
		this.lifecycleManagement = lifecycleManagement;
	}

	String getHost() {
		return this.host;
	}

	void setHost(String host) {
		this.host = host;
	}

	Startup getStartup() {
		return this.startup;
	}

	Shutdown getShutdown() {
		return this.shutdown;
	}

	Profiles getProfiles() {
		return this.profiles;
	}

	Skip getSkip() {
		return this.skip;
	}

	static DockerComposeProperties get(Binder binder) {
		return binder.bind(NAME, DockerComposeProperties.class).orElseGet(DockerComposeProperties::new);
	}

	/**
	 * Startup properties.
	 */
	static class Startup {

		/**
		 * The command used to start docker compose.
		 */
		private StartupCommand command = StartupCommand.UP;

		StartupCommand getCommand() {
			return this.command;
		}

		void setCommand(StartupCommand command) {
			this.command = command;
		}

	}

	/**
	 * Shutdown properties.
	 */
	static class Shutdown {

		/**
		 * The command used to stop docker compose.
		 */
		private ShutdownCommand command = ShutdownCommand.DOWN;

		/**
		 * The timeout for stopping docker compose. Use '0' for forced stop.
		 */
		private Duration timeout = Duration.ofSeconds(10);

		ShutdownCommand getCommand() {
			return this.command;
		}

		void setCommand(ShutdownCommand command) {
			this.command = command;
		}

		Duration getTimeout() {
			return this.timeout;
		}

		void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	/**
	 * Profiles properties.
	 */
	static class Profiles {

		/**
		 * Docker compose profiles that should be active.
		 */
		private Set<String> active = new LinkedHashSet<>();

		Set<String> getActive() {
			return this.active;
		}

		void setActive(Set<String> active) {
			this.active = active;
		}

	}

	/**
	 * Skip options.
	 */
	static class Skip {

		/**
		 * Whether to skip in tests.
		 */
		private boolean inTests = true;

		boolean isInTests() {
			return this.inTests;
		}

		void setInTests(boolean inTests) {
			this.inTests = inTests;
		}

	}

}
