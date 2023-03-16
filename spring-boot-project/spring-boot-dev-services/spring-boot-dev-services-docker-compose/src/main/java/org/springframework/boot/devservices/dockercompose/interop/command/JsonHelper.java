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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * Helper class for JSON serialization/deserialization.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
final class JsonHelper {

	private JsonHelper() {
	}

	/**
	 * Deserializes a JSON list to DTOs. Handles JSON arrays and multiple JSON objects in
	 * separate lines.
	 * @param mapper object mapper
	 * @param json json to parse
	 * @param clazz type of the DTO
	 * @param <T> type of the DTO
	 * @return list of DTOs
	 * @throws JsonProcessingException if something went wrong
	 */
	static <T> List<T> deserializeList(ObjectMapper mapper, String json, Class<T> clazz)
			throws JsonProcessingException {
		if (json.startsWith("[")) {
			CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
			return mapper.readValue(json, javaType);
		}
		try (BufferedReader reader = new BufferedReader(new StringReader(json))) {
			String line;
			List<T> result = new ArrayList<>();
			while ((line = reader.readLine()) != null) {
				result.add(mapper.readValue(line, clazz));
			}
			return result;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to parse JSON: '%s'".formatted(json), ex);
		}
	}

}
