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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonHelper}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class JsonHelperTests {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void array() throws JsonProcessingException {
		String json = """
				[{ "value": 1 }, { "value": 2 }]
				""".trim();
		List<Dto> dtos = JsonHelper.deserializeList(this.mapper, json, Dto.class);
		assertThat(dtos).containsExactly(new Dto(1), new Dto(2));
	}

	@Test
	void lines() throws JsonProcessingException {
		String json = """
				{ "value": 1 }
				{ "value": 2 }
				""".trim();
		List<Dto> dtos = JsonHelper.deserializeList(this.mapper, json, Dto.class);
		assertThat(dtos).containsExactly(new Dto(1), new Dto(2));
	}

	record Dto(int value) {
	}

}
