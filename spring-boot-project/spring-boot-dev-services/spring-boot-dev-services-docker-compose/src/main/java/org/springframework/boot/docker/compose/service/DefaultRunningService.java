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

package org.springframework.boot.docker.compose.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.CollectionUtils;

/**
 * Default {@link RunningService} implementation backed by {@link DockerCli} responses.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DefaultRunningService implements RunningService, OriginProvider {

	private final Origin origin;

	private final String name;

	private final ImageReference image;

	private final DefaultRunningServicePorts ports;

	private final Map<String, String> labels;

	private Map<String, String> env;

	DefaultRunningService(DockerComposeFile composeFile, DockerCliComposePsResponse psResponse,
			DockerCliInspectResponse inspectResponse, String hostname) {
		this.origin = new DockerComposeOrigin(composeFile, psResponse.name());
		this.name = psResponse.name();
		this.image = ImageReference.parse(psResponse.image());
		this.ports = new DefaultRunningServicePorts(inspectResponse);
		this.env = envToMap(inspectResponse.config().env());
		this.labels = Collections.unmodifiableMap(inspectResponse.config().labels());
	}

	private Map<String, String> envToMap(List<String> env) {
		if (CollectionUtils.isEmpty(env)) {
			return Collections.emptyMap();
		}
		Map<String, String> result = new LinkedHashMap<>();
		env.stream().map(this::envItemToMapEntry).forEach((entry) -> result.put(entry.getKey(), entry.getValue()));
		return Collections.unmodifiableMap(result);
	}

	private Map.Entry<String, String> envItemToMapEntry(String item) {
		int index = item.indexOf('=');
		if (index != -1) {
			String key = item.substring(0, index);
			String value = item.substring(index + 1);
			return Map.entry(key, value);
		}
		return Map.entry(item, null);
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public ImageReference image() {
		return this.image;
	}

	@Override
	public String host() {
		// FIXME copy logic from ServiceMapper
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Ports ports() {
		return this.ports;
	}

	@Override
	public Map<String, String> env() {
		return this.env;
	}

	@Override
	public Map<String, String> labels() {
		return this.labels;
	}

	@Override
	public String toString() {
		return this.name;
	}

}
