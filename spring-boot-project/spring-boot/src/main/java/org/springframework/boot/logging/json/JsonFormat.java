/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.json;

/**
 * JSON format.
 *
 * @param <E> the event type, dependent on the logging system
 * @author Moritz Halbritter
 * @since 3.3.0
 */
public interface JsonFormat<E> {

	default void setPid(long pid) {
	}

	default void setServiceName(String serviceName) {
	}

	default void setServiceVersion(String serviceVersion) {
	}

	/**
	 * Returns the fields to write.
	 * @param event the event to log
	 * @return the fields to write
	 */
	Iterable<Field> getFields(E event);

}
