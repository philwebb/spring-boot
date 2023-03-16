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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;

/**
 * A connection to a Zipkin service.
 *
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public interface ZipkinServiceConnection extends ServiceConnection {

	/**
	 * Hostname of the Zipkin service.
	 * @return the hostname
	 */
	String getHost();

	/**
	 * Port of the Zipkin service.
	 * @return the port
	 */
	int getPort();

	/**
	 * Path of the span reporting endpoint of the Zipkin service.
	 * @return the span path
	 */
	String getSpanPath();

	/**
	 * The endpoint for the span reporting.
	 * @return the endpoint
	 */
	default String getSpanEndpoint() {
		return "http://%s:%d%s".formatted(getHost(), getPort(), getSpanPath());
	}

}
