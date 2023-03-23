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

package org.springframework.boot.devservices.xdockercompose.interop;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.Assert;

/**
 * A running docker compose service.
 *
 * @param composeConfigFile docker compose config file in which the service has been
 * defined or {@code null}
 * @param name name of the service
 * @param image image of the service
 * @param host host of the service
 * @param ports mapping from container port to host port
 * @param env environment of the service
 * @param labels labels of the service
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
public record RunningService(Path composeConfigFile, String name, DockerImageName image, String host,
		Map<Integer, Port> ports, Map<String, String> env, Map<String, String> labels) implements OriginProvider {

	public DockerImageName originalImage() {
		return this.image;
	}

	/**
	 * Returns the image of the service. If the label
	 * {@code org.springframework.boot.image-override} is set, this value will be
	 * returned.
	 * @return the image of the service
	 * @see #originalImage()
	 */
	public DockerImageName image() {
		// FIXME I think we should have a different name for this
		// org.springframework.boot.service=redis
		String override = this.labels.get("org.springframework.boot.image-override");
		if (override != null) {
			return DockerImageName.parse(override);
		}
		return this.image;
	}

	public Port getMappedPort(int sourcePort) {
		// FIXME DC
		Port mappedPort = ports().get(sourcePort);
		Assert.state(mappedPort != null, () -> "Port %d has not been mapped".formatted(sourcePort));
		return mappedPort;
	}

	/**
	 * Returns whether this service should be ignored. A service is considered ignored if
	 * the {@code org.springframework.boot.ignore} label is set.
	 * @return whether this service should be ignored
	 */
	public boolean ignore() {
		return this.labels.containsKey("org.springframework.boot.ignore");
	}

	/**
	 * Returns whether this service should be readiness checked. The readiness check is
	 * disabled if the {@code org.springframework.boot.readiness-check.disable} label is
	 * set or if the service is {@link #ignore() ignored}.
	 * @return whether this service should be readiness checked
	 */
	public boolean readinessCheck() {
		return !ignore() && !this.labels.containsKey("org.springframework.boot.readiness-check.disable");
	}

	@Override
	public Origin getOrigin() {
		return (this.composeConfigFile != null) ? new DockerComposeOrigin(this.composeConfigFile, this.name) : null;
	}

	// FIXME toString

}
