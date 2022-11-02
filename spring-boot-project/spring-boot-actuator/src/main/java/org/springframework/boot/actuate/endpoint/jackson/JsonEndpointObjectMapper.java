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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Strategy used to supply the {@link ObjectMapper} that should be used for
 * {@link JsonEndpointResponse @JsonEndpointResponse} annotated methods.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@FunctionalInterface
public interface JsonEndpointObjectMapper {

	/**
	 * Return the JSON endpoint object mapper.
	 * @return the {@link ObjectMapper} instance
	 */
	ObjectMapper get();

}
