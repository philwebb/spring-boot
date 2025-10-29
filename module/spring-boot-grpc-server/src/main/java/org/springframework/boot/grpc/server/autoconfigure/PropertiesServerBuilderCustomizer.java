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

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties.Inbound;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerProperties.Keepalive;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.util.unit.DataSize;

/**
 * {@link ServerBuilderCustomizer} that maps {@link GrpcServerProperties} to a
 * {@link ManagedChannelBuilder}.
 *
 * @param <T> the type of server builder
 * @author Chris Bono
 * @author Phillip Webb
 */
class PropertiesServerBuilderCustomizer<T extends ServerBuilder<T>> implements ServerBuilderCustomizer<T> {

	private final GrpcServerProperties properties;

	private final Scope scope;

	PropertiesServerBuilderCustomizer(GrpcServerProperties properties) {
		this(properties, Scope.ALL);
	}

	PropertiesServerBuilderCustomizer(GrpcServerProperties properties, Scope scope) {
		this.properties = properties;
		this.scope = scope;
	}

	@Override
	public void customize(T builder) {
		if (this.scope == Scope.ALL) {
			mapKeepalive(this.properties.getKeepAlive(), builder);
		}
		mapInbound(this.properties.getInbound(), builder);
	}

	private void mapKeepalive(Keepalive properties, T builder) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties::getTime).to(durationProperty(builder::keepAliveTime));
		map.from(properties::getTimeout).to(durationProperty(builder::keepAliveTimeout));
		map.from(properties.getConnection()::getMaxIdleTime).to(durationProperty(builder::maxConnectionIdle));
		map.from(properties.getConnection()::getMaxAge).to(durationProperty(builder::maxConnectionAge));
		map.from(properties.getConnection()::getGracePeriod).to(durationProperty(builder::maxConnectionAgeGrace));
		map.from(properties.getPermit()::getTime).to(durationProperty(builder::permitKeepAliveTime));
		map.from(properties.getPermit()::isWithoutCalls).to(builder::permitKeepAliveWithoutCalls);
	}

	private void mapInbound(Inbound properties, T builder) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties.getMessage()::getMaxSize).asInt(DataSize::toBytes).to(builder::maxInboundMessageSize);
		map.from(properties.getMetadata()::getMaxSize).asInt(DataSize::toBytes).to(builder::maxInboundMetadataSize);
	}

	private Consumer<Duration> durationProperty(BiConsumer<Long, TimeUnit> setter) {
		return (duration) -> setter.accept(duration.toNanos(), TimeUnit.NANOSECONDS);
	}

	/**
	 * The scope of the mapping to apply.
	 */
	enum Scope {

		/**
		 * Map all supported properties.
		 */
		ALL,

		/**
		 * Map only inbound properties.
		 */
		INBOUND_ONLY

	}

}
