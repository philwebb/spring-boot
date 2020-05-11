/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import io.undertow.server.HttpHandler;

/**
 * A manager for an {@link HttpHandler}.
 *
 * @author Andy Wilkinson
 * @since 2.3.0
 */
public interface HandlerManager {

	/**
	 * Starts the handler manager, creating and returning the {@link HttpHandler} that it
	 * manages.
	 * @return the {@link HttpHandler}
	 */
	HttpHandler start();

	/**
	 * Stops the handler manager, closing any resources used by its {@link HttpHandler}.
	 */
	void stop();

	/**
	 * Extracts an instance of the given {@code type}. Returns {@code null} if no such
	 * instance can be extracted.
	 * @param <T> type to extract
	 * @param type class of the instance to extract
	 * @return the extracted instance, or {@code null}
	 */
	<T> T extract(Class<T> type);

}
