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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * DTO for 'docker compose ps' output.
 *
 * @param id container id
 * @param state container state
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record ComposePsOutput(@JsonProperty("ID") String id, @JsonProperty("State") String state) {
	static List<ComposePsOutput> parse(ObjectMapper mapper, String json) throws JsonProcessingException {
		return JsonHelper.deserializeList(mapper, json, ComposePsOutput.class);
	}

	public boolean running() {
		return !"exited".equals(this.state);
	}
}
