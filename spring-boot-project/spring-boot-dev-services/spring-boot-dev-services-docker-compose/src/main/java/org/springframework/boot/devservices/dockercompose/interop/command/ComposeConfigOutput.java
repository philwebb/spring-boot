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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DTO for 'docker compose config' output.
 *
 * @param name project name
 * @param services services
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record ComposeConfigOutput(@JsonProperty("name") String name,
		@JsonProperty("services") Map<String, Service> services) {

	static ComposeConfigOutput parse(ObjectMapper mapper, String json) throws JsonProcessingException {
		return mapper.readValue(json, ComposeConfigOutput.class);
	}

	/**
	 * Docker compose service.
	 *
	 * @param image image
	 */
	public record Service(@JsonProperty("image") String image) {
	}
}
