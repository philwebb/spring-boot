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

package org.springframework.boot.devservices.xdockercompose.readiness;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * Is thrown if the timeout for the readiness check is reached.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public class ReadinessTimeoutException extends RuntimeException {

	private final RunningService service;

	private final Duration timeout;

	public ReadinessTimeoutException(RunningService service, Duration timeout, Throwable cause) {
		super("Timeout of %s reached while waiting for service '%s' readiness".formatted(timeout, service.logicalTypeName()),
				cause);
		this.service = service;
		this.timeout = timeout;
	}

	/**
	 * @param timeout2
	 * @param exceptions
	 */
	public ReadinessTimeoutException(Duration timeout2, List<ServiceNotReadyException> exceptions) {
		// TODO Auto-generated constructor stub
	}

	public RunningService getService() {
		return this.service;
	}

	public Duration getTimeout() {
		return this.timeout;
	}

}
