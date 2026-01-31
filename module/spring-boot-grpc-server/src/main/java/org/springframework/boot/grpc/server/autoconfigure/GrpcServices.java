/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.stream.Stream;

import io.grpc.ServerServiceDefinition;

import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * Internal utility class to discover and configure gRPC services.
 *
 * @author Phillip Webb
 */
class GrpcServices {

	private final GrpcServiceDiscoverer discoverer;

	private final GrpcServiceConfigurer configurer;

	GrpcServices(GrpcServiceDiscoverer discoverer, GrpcServiceConfigurer configurer) {
		this.discoverer = discoverer;
		this.configurer = configurer;
	}

	void addToServerFactory(GrpcServerFactory serverFactory) {
		findConfigured(serverFactory).forEach(serverFactory::addService);
	}

	Stream<ServerServiceDefinition> findConfigured(GrpcServerFactory serverFactory) {
		return this.discoverer.findServices().stream().map((spec) -> this.configurer.configure(spec, serverFactory));
	}

}
