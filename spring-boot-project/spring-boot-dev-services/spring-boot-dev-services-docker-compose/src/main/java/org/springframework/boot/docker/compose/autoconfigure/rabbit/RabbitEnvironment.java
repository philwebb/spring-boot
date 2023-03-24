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

package org.springframework.boot.docker.compose.autoconfigure.rabbit;

import java.util.Map;

class RabbitEnvironment {

	private final String username;

	private final String password;

	public RabbitEnvironment(Map<String, String> env) {
		this.username = env.getOrDefault("RABBITMQ_DEFAULT_USER", "guest");
		this.password = env.getOrDefault("RABBITMQ_DEFAULT_PASS", "guest");
	}

	String getUsername() {
		return this.username;
	}

	String getPassword() {
		return this.password;
	}

}
