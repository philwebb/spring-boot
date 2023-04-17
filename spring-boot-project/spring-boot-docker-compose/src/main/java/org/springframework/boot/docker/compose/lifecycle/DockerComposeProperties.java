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

package org.springframework.boot.docker.compose.lifecycle;

import java.io.File;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.LogLevel;

/**
 * Configuration properties for the 'docker compose'.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
@ConfigurationProperties(DockerComposeProperties.NAME)
public class DockerComposeProperties {

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

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public File getFile() {
		return this.file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public LifecycleManagement getLifecycleManagement() {
		return this.lifecycleManagement;
	}

	public void setLifecycleManagement(LifecycleManagement lifecycleManagement) {
		this.lifecycleManagement = lifecycleManagement;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Startup getStartup() {
		return this.startup;
	}

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public Profiles getProfiles() {
		return this.profiles;
	}

	public Skip getSkip() {
		return this.skip;
	}

	static DockerComposeProperties get(Binder binder) {
		return binder.bind(NAME, DockerComposeProperties.class).orElseGet(DockerComposeProperties::new);
	}

	/**
	 * Startup properties.
	 */
	public static class Startup {

		/**
		 * Command used to start docker compose.
		 */
		private StartupCommand command = StartupCommand.UP;

		/**
		 * Log level for output.
		 */
		private LogLevel logLevel = LogLevel.INFO;

		public StartupCommand getCommand() {
			return this.command;
		}

		public void setCommand(StartupCommand command) {
			this.command = command;
		}

		public LogLevel getLogLevel() {
			return this.logLevel;
		}

		public void setLogLevel(LogLevel logLevel) {
			this.logLevel = logLevel;
		}

	}

	/**
	 * Shutdown properties.
	 */
	public static class Shutdown {

		/**
		 * Command used to stop docker compose.
		 */
		private ShutdownCommand command = ShutdownCommand.DOWN;

		/**
		 * Timeout for stopping docker compose. Use '0' for forced stop.
		 */
		private Duration timeout = Duration.ofSeconds(10);

		public ShutdownCommand getCommand() {
			return this.command;
		}

		public void setCommand(ShutdownCommand command) {
			this.command = command;
		}

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	/**
	 * Profiles properties.
	 */
	public static class Profiles {

		/**
		 * Docker compose profiles that should be active.
		 */
		private Set<String> active = new LinkedHashSet<>();

		public Set<String> getActive() {
			return this.active;
		}

		public void setActive(Set<String> active) {
			this.active = active;
		}

	}

	/**
	 * Skip options.
	 */
	public static class Skip {

		/**
		 * Whether to skip in tests.
		 */
		private boolean inTests = true;

		public boolean isInTests() {
			return this.inTests;
		}

		public void setInTests(boolean inTests) {
			this.inTests = inTests;
		}

	}

}
