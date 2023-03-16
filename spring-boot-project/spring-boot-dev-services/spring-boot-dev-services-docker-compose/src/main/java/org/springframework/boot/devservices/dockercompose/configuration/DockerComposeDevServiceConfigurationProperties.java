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

package org.springframework.boot.devservices.dockercompose.configuration;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Docker Compose dev services.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
@ConfigurationProperties(prefix = DockerComposeDevServiceConfigurationProperties.PREFIX)
public class DockerComposeDevServiceConfigurationProperties {

	/**
	 * Prefix for the configuration properties.
	 */
	public static final String PREFIX = "spring.dev-services.docker-compose";

	/**
	 * Path to the docker compose configuration file to use or {@code null}.
	 */
	private String configFile;

	private Readiness readiness = new Readiness();

	/**
	 * Hostname or IP of the machine where the docker containers are started or
	 * {@code null}.
	 */
	private String dockerHostname;

	/**
	 * Docker compose lifecycle management.
	 */
	private LifecycleManagement lifecycleManagement = LifecycleManagement.START_AND_STOP;

	/**
	 * Docker compose stop mode.
	 */
	private StopMode stopMode = StopMode.STOP;

	/**
	 * Active docker compose profiles.
	 */
	private Set<String> activeProfiles = new HashSet<>();

	/**
	 * Whether to run the dev services in tests.
	 */
	private boolean runInTests = false;

	public boolean isRunInTests() {
		return this.runInTests;
	}

	public void setRunInTests(boolean runInTests) {
		this.runInTests = runInTests;
	}

	public Set<String> getActiveProfiles() {
		return this.activeProfiles;
	}

	public void setActiveProfiles(Set<String> activeProfiles) {
		this.activeProfiles = activeProfiles;
	}

	public LifecycleManagement getLifecycleManagement() {
		return this.lifecycleManagement;
	}

	public void setLifecycleManagement(LifecycleManagement lifecycleManagement) {
		this.lifecycleManagement = lifecycleManagement;
	}

	public String getDockerHostname() {
		return this.dockerHostname;
	}

	public void setDockerHostname(String dockerHostname) {
		this.dockerHostname = dockerHostname;
	}

	public String getConfigFile() {
		return this.configFile;
	}

	public void setConfigFile(String configFile) {
		this.configFile = configFile;
	}

	public Readiness getReadiness() {
		return this.readiness;
	}

	public void setReadiness(Readiness readiness) {
		this.readiness = readiness;
	}

	public StopMode getStopMode() {
		return this.stopMode;
	}

	public void setStopMode(StopMode stopMode) {
		this.stopMode = stopMode;
	}

	public static class Readiness {

		/**
		 * TCP reeadiness check configuration.
		 */
		private Tcp tcp = new Tcp();

		/**
		 * Timeout of the readiness checks.
		 */
		private Duration timeout = Duration.ofSeconds(30);

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		public Tcp getTcp() {
			return this.tcp;
		}

		public void setTcp(Tcp tcp) {
			this.tcp = tcp;
		}

		public static class Tcp {

			/**
			 * Timeout for connections.
			 */
			private Duration connectTimeout = Duration.ofMillis(200);

			/**
			 * Timeout for reads.
			 */
			private Duration readTimeout = Duration.ofMillis(200);

			public Duration getConnectTimeout() {
				return this.connectTimeout;
			}

			public void setConnectTimeout(Duration connectTimeout) {
				this.connectTimeout = connectTimeout;
			}

			public Duration getReadTimeout() {
				return this.readTimeout;
			}

			public void setReadTimeout(Duration readTimeout) {
				this.readTimeout = readTimeout;
			}

		}

	}

}
