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

package org.springframework.boot.devservices.dockercompose;

import java.util.List;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.core.env.Environment;

/**
 * Provides {@link ServiceConnection ServiceConnections} for running Docker Compose
 * services. The implementations of this class can use the following types in their
 * constructor:
 * <ul>
 * <li>{@link Environment}</li>
 * <li>{@link Binder}</li>
 * <li>{@link ClassLoader}</li>
 * </ul>
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface RunningServiceServiceConnectionProvider {

	/**
	 * Returns a list of {@link ServiceConnection service connections} for the given
	 * {@code services}.
	 * @param services the running Docker Compose services
	 * @return the list of service connections
	 */
	List<? extends ServiceConnection> provideServiceConnection(List<RunningService> services);

}
