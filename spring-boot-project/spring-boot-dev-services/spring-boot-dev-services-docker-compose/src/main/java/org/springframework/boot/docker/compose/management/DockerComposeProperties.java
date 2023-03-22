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
	 * Path to a specific docker compose configuration file.
	 */
	private File file;

	/**
	 * Docker compose lifecycle management.
	 */
	private LifecycleManagement lifecycleManagement;

	/**
	 * Hostname or IP of the machine where the docker containers are started.
	 */
	private String hostname;

	/**
	 * Start configuration.
	 */
	private final Start start = new Start();

	/**
	 * Stop configuration.
	 */
	private final Stop stop = new Stop();

	/**
	 * Profiles configuration.
	 */
	private final Profiles profiles = new Profiles();

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

	String getHostname() {
		return this.hostname;
	}

	void setHostname(String hostname) {
		this.hostname = hostname;
	}

	Start getStart() {
		return this.start;
	}

	Stop getStop() {
		return this.stop;
	}

	Profiles getProfiles() {
		return this.profiles;
	}

	static class Profiles {

		/**
		 * Docker compose profiles that should be active.
		 */
		private Set<String> active;

		Set<String> getActive() {
			return this.active;
		}

		void setActive(Set<String> active) {
			this.active = active;
		}

	}

	static class Start {

		/**
		 * The command used to start docker compose.
		 */
		private StartCommand command = StartCommand.UP;

		StartCommand getCommand() {
			return this.command;
		}

		void setCommand(StartCommand command) {
			this.command = command;
		}

	}

	static class Stop {

		/**
		 * The command used to stop docker compose.
		 */
		private StopCommand command = StopCommand.STOP;

		/**
		 * The timeout for stopping docker compose. Use '0' for forced stop.
		 */
		private Duration timeout = Duration.ofSeconds(10);

		StopCommand getCommand() {
			return this.command;
		}

		void setCommand(StopCommand command) {
			this.command = command;
		}

		Duration getTimeout() {
			return this.timeout;
		}

		void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	static DockerComposeProperties get(Binder binder) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
