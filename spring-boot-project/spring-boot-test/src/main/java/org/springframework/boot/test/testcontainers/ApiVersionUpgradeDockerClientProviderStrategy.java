/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.testcontainers;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.RemoteApiVersion;

import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * {@link DockerClientProviderStrategy} that upgrades the Docker API version when it
 * hasn't been specified and Testcontainers v1 is being used.
 * <p>
 * To disable this feature, create a {@code docker-java.properties} containing
 * {@code api.version.upgrade=false}.
 *
 * @author Phillip Webb
 */
public class ApiVersionUpgradeDockerClientProviderStrategy extends DockerClientProviderStrategy {

	private static final Pattern DOCKER_JAVA_1_DOT_X = Pattern.compile("1\\.\\d+\\..*");

	public ApiVersionUpgradeDockerClientProviderStrategy() {
		this(DefaultDockerClientConfig.createDefaultConfigBuilder());
	}

	public ApiVersionUpgradeDockerClientProviderStrategy(DefaultDockerClientConfig.Builder builder) {
		if (canUpgrade() && builder.build().getApiVersion() == RemoteApiVersion.UNKNOWN_VERSION) {
			System.setProperty("api.version", "1.44");
		}
	}

	private boolean canUpgrade() {
		Package dockerJavaPackage = DefaultDockerClientConfig.class.getPackage();
		String dockerJavaVersion = (dockerJavaPackage != null) ? dockerJavaPackage.getImplementationVersion() : null;
		if (dockerJavaPackage == null || DOCKER_JAVA_1_DOT_X.matcher(dockerJavaVersion).matches()) {
			try {
				Properties properties = PropertiesLoaderUtils.loadAllProperties("/docker-java.properties",
						DefaultDockerClientConfig.class.getClassLoader());
				return Boolean.parseBoolean((String) properties.getOrDefault("docker.api.upgrade", "true"));
			}
			catch (IOException ex) {
			}
		}
		return false;
	}

	@Override
	protected boolean isApplicable() {
		return false;
	}

	@Override
	protected boolean isPersistable() {
		return false;
	}

	@Override
	public String getDescription() {
		return "Docker API Version Upgrade";
	}

	@Override
	public TransportConfig getTransportConfig() throws InvalidConfigurationException {
		throw new InvalidConfigurationException("Unexpected call to getTransportConfig()");
	}

}
