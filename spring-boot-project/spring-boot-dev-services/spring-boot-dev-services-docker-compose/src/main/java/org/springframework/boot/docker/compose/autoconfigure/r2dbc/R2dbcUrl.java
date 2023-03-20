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

package org.springframework.boot.docker.compose.autoconfigure.r2dbc;

import org.springframework.boot.devservices.dockercompose.interop.RunningService;

/**
 * @author pwebb
 */
public class R2dbcUrl {

	/**
	 * @param source
	 * @param string
	 * @param mariadbPort
	 * @param database
	 */
	public R2dbcUrl(RunningService source, String string, int mariadbPort, String database) {
		// TODO Auto-generated constructor stub
		String parameters = source.labels().get("org.springframework.boot.r2dbc.parameters");

	}

}
