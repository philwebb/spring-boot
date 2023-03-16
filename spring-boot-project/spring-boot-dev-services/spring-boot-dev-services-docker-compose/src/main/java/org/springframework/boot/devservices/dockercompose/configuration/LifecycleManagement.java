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

package org.springframework.boot.devservices.dockercompose.configuration;

/**
 * Docker compose lifecycle management.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public enum LifecycleManagement {

	/**
	 * Don't start or stop docker compose.
	 */
	NONE(false, false),
	/**
	 * Only start docker compose if it's not running.
	 */
	START_ONLY(true, false),
	/**
	 * Start and stop docker compose if it's not running.
	 */
	START_AND_STOP(true, true);

	private final boolean start;

	private final boolean stop;

	LifecycleManagement(boolean start, boolean stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Returns whether docker compose should be started.
	 * @return whether docker compose should be started.
	 */
	public boolean isStart() {
		return this.start;
	}

	/**
	 * Returns whether docker compose should be stopped.
	 * @return whether docker compose should be stopped
	 */
	public boolean isStop() {
		return this.stop;
	}

}
