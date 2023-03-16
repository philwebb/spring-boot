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

package org.springframework.boot.autoconfigure.amqp;

import java.util.List;

import org.springframework.boot.origin.Origin;

/**
 * A {@link RabbitServiceConnection} for tests.
 *
 * @author Moritz Halbritter
 */
class TestRabbitServiceConnection implements RabbitServiceConnection {

	@Override
	public String getUsername() {
		return "user-1";
	}

	@Override
	public String getPassword() {
		return "password-1";
	}

	@Override
	public String getVirtualHost() {
		return "/vhost-1";
	}

	@Override
	public List<Address> getAddresses() {
		return List.of(new Address("rabbit.example.com", 12345), new Address("rabbit2.example.com", 23456));
	}

	@Override
	public String getName() {
		return "test-rabbit-service-connection";
	}

	@Override
	public Origin getOrigin() {
		return null;
	}

}
