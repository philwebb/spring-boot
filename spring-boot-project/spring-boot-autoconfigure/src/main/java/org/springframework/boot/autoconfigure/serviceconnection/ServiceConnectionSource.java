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

package org.springframework.boot.autoconfigure.serviceconnection;

import org.springframework.boot.origin.Origin;

/**
 * A source from which a {@link ServiceConnection} can be created.
 *
 * @param input the input from which to create the service connection
 * @param name the name of the service connection
 * @param origin the origin of the service connection
 * @param connectionType the required type of the service connection
 * @param <I> the type of the input
 * @param <SC> the type of the service connection
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @see ServiceConnectionFactory
 * @since 3.1.0
 */
public record ServiceConnectionSource<I, SC extends ServiceConnection>(I input, String name, Origin origin,
		Class<SC> connectionType) {

}
