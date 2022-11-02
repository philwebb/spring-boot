/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.springframework.boot.actuate.endpoint.jackson.JsonSerializableEndpointResponseBody.Serializer;

/**
 * Wrapper to hold an endpoint response body and the {@link ObjectMapper} that should
 * serialize it.
 *
 * @author Phillip Webb
 */
@JsonSerialize(using = Serializer.class)
public class JsonSerializableEndpointResponseBody {

	private final Object body;

	private final ObjectMapper objectMapper;

	JsonSerializableEndpointResponseBody(ObjectMapper objectMapper, Object body) {
		this.objectMapper = objectMapper;
		this.body = body;
	}

	/**
	 * {@link JsonSerializer} for {@link JsonSerializableEndpointResponseBody} that
	 * delegates to the contained {@link ObjectMapper}.
	 */
	static class Serializer extends JsonSerializer<JsonSerializableEndpointResponseBody> {

		@Override
		public void serialize(JsonSerializableEndpointResponseBody value, JsonGenerator gen,
				SerializerProvider serializers) throws IOException {
			value.objectMapper.writer().writeValue(gen, value.body);
		}

	}

}
