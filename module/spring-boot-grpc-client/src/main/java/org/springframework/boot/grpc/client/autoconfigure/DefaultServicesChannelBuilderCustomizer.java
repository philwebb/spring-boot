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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * {@link GrpcChannelBuilderCustomizer} to apply
 * {@link GrpcChannelBuilderDefaultServiceConfigCustomizer} beans and default service
 * config items from {@link GrpcClientProperties}.
 * <p>
 * This customizer is always applied last.
 *
 * @param <T> the builder type
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class DefaultServicesChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>>
		implements GrpcChannelBuilderCustomizer<T> {

	private final ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers;

	private final GrpcClientProperties properties;

	public DefaultServicesChannelBuilderCustomizer(GrpcClientProperties properties,
			ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers) {
		this.customizers = customizers;
		this.properties = properties;
	}

	@Override
	public void customize(String target, T builder) {
		Map<String, Object> defaultServiceConfig = new LinkedHashMap<>();
		customize(target, defaultServiceConfig);
		this.customizers.orderedStream().forEach((customizer) -> customizer.customize(target, defaultServiceConfig));
		if (!defaultServiceConfig.isEmpty()) {
			builder.defaultServiceConfig(defaultServiceConfig);
		}
	}

	private void customize(String target, Map<String, Object> defaultServiceConfig) {
		Channel channel = this.properties.getChannel().get(target);
		channel = (channel != null) ? channel : this.properties.getChannel().get("default");
		if (channel != null && channel.getHealth().isEnabled()) {
			String serviceName = channel.getHealth().getServiceName();
			Map<String, String> healthCheckConfig = Map.of("serviceName", (serviceName != null) ? serviceName : "");
			defaultServiceConfig.put("healthCheckConfig", healthCheckConfig);
		}
	}

}