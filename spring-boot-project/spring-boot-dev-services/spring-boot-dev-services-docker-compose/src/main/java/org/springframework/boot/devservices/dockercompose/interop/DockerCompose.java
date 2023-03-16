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

package org.springframework.boot.devservices.dockercompose.interop;

import java.util.List;

import org.springframework.boot.devservices.dockercompose.configuration.StopMode;

/**
 * Docker compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public interface DockerCompose {

	/**
	 * Returns all running services.
	 * @return the running services
	 */
	List<RunningService> listRunningServices();

	/**
	 * Returns all defined services.
	 * @return the defined services
	 */
	List<DefinedService> listDefinedServices();

	/**
	 * Checks whether docker compose is running.
	 * @return whether docker compose is running
	 */
	default boolean isRunning() {
		return isRunning(listDefinedServices(), listRunningServices());
	}

	/**
	 * Checks whether docker compose is running.
	 * @param definedServices the defined services
	 * @param runningServices the running services
	 * @return whether docker compose is running
	 */
	boolean isRunning(List<DefinedService> definedServices, List<RunningService> runningServices);

	/**
	 * Starts all services and waits until they are started and healthy.
	 */
	void startServices();

	/**
	 * Stops all services.
	 * @param stopMode the stop mode
	 */
	void stopServices(StopMode stopMode);

}
