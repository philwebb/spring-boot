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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.util.StringUtils;

/**
 * DTO for 'docker inspect' output.
 *
 * @param id container id
 * @param config config
 * @param hostConfig host config
 * @param networkSettings network settings
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record DockerInspectOutput(@JsonProperty("Id") String id, @JsonProperty("Config") Config config,
		@JsonProperty("NetworkSettings") NetworkSettings networkSettings,
		@JsonProperty("HostConfig") HostConfig hostConfig) {

	static List<DockerInspectOutput> parse(ObjectMapper mapper, String json) throws JsonProcessingException {
		return JsonHelper.deserializeList(mapper, json, DockerInspectOutput.class);
	}

	public record HostConfig(@JsonProperty("NetworkMode") String networkMode) {
		public boolean isHostNetwork() {
			return "host".equals(this.networkMode);
		}
	}

	public record Config(@JsonProperty("Image") String image, @JsonProperty("Labels") Map<String, String> labels,
			@JsonProperty("ExposedPorts") Map<String, ExposedPort> exposedPorts,
			@JsonProperty("Env") List<String> env) {
		public Map<String, String> envAsMap() {
			if (this.env == null || this.env.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<String, String> result = new HashMap<>();
			for (String env : this.env) {
				int equals = env.indexOf('=');
				if (equals == -1) {
					result.put(env, null);
				}
				else {
					String key = env.substring(0, equals);
					String value = env.substring(equals + 1);
					result.put(key, value);
				}
			}
			return result;
		}
	}

	public record ExposedPort() {
	}

	public record NetworkSettings(@JsonProperty("Ports") Map<String, List<PortDto>> ports) {
		/**
		 * Port.
		 *
		 * @param hostIp host ip or {@code null}
		 * @param hostPort host port
		 */
		public record PortDto(@JsonProperty("HostIp") String hostIp, @JsonProperty("HostPort") String hostPort) {
			public boolean isIpV4() {
				if (!StringUtils.hasLength(this.hostIp)) {
					return true;
				}
				return this.hostIp.contains(".");
			}

			public int hostPortAsInt() {
				return Integer.parseInt(this.hostPort);
			}
		}
	}
}
