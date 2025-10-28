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
import java.util.stream.Stream;

import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link DefaultServicesChannelBuilderCustomizer}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class DefaultServicesChannelBuilderCustomizerTests {

	@Test
	void customizeAppliesGrpcChannelBuilderDefaultServiceConfigCustomizers() {
		GrpcChannelBuilderDefaultServiceConfigCustomizer customizer1 = (target, defaultServiceConfig) -> {
			defaultServiceConfig.put("c", "v1");
			defaultServiceConfig.put("c1", "v1");
		};
		GrpcChannelBuilderDefaultServiceConfigCustomizer customizer2 = (target, defaultServiceConfig) -> {
			defaultServiceConfig.put("c", "v2");
			defaultServiceConfig.put("c2", "v2");
		};
		ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers = mock();
		given(customizers.orderedStream()).willReturn(Stream.of(customizer1, customizer2));
		DefaultServicesChannelBuilderCustomizer builderCustomizer = new DefaultServicesChannelBuilderCustomizer<>(
				new GrpcClientProperties(), customizers);
		ManagedChannelBuilder builder = mock();
		builderCustomizer.customize("target", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("c", "v2");
		expected.put("c1", "v1");
		expected.put("c2", "v2");
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void customizeWhenHasChannelHealthAddsHealthServiceConfig() {
		ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers = mock();
		given(customizers.orderedStream()).willReturn(Stream.empty());
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		channel.getHealth().setServiceName("testservice");
		properties.getChannel().put("test", channel);
		DefaultServicesChannelBuilderCustomizer builderCustomizer = new DefaultServicesChannelBuilderCustomizer<>(
				properties, customizers);
		ManagedChannelBuilder builder = mock();
		builderCustomizer.customize("test", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", "testservice"));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void customizeWhenHasDefaultHealthAddsHealthServiceConfig() {
		ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers = mock();
		given(customizers.orderedStream()).willReturn(Stream.empty());
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		channel.getHealth().setServiceName("testdefaultservice");
		properties.getChannel().put("default", channel);
		DefaultServicesChannelBuilderCustomizer builderCustomizer = new DefaultServicesChannelBuilderCustomizer<>(
				properties, customizers);
		ManagedChannelBuilder builder = mock();
		builderCustomizer.customize("test", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", "testdefaultservice"));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void customizeWhenHealthEnabledAndNoServiceNameAddsHealthConfig() {
		ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers = mock();
		given(customizers.orderedStream()).willReturn(Stream.empty());
		GrpcClientProperties properties = new GrpcClientProperties();
		Channel channel = new Channel();
		channel.getHealth().setEnabled(true);
		properties.getChannel().put("test", channel);
		DefaultServicesChannelBuilderCustomizer builderCustomizer = new DefaultServicesChannelBuilderCustomizer<>(
				properties, customizers);
		ManagedChannelBuilder builder = mock();
		builderCustomizer.customize("test", builder);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("healthCheckConfig", Map.of("serviceName", ""));
		then(builder).should().defaultServiceConfig(expected);
	}

	@Test
	void customizeWhenNoCustomizersOrHealthDoesSetDefaultServiceConfig() {
		ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers = mock();
		given(customizers.orderedStream()).willReturn(Stream.empty());
		DefaultServicesChannelBuilderCustomizer builderCustomizer = new DefaultServicesChannelBuilderCustomizer<>(
				new GrpcClientProperties(), customizers);
		ManagedChannelBuilder builder = mock();
		builderCustomizer.customize("test", builder);
		then(builder).should(never()).defaultServiceConfig(any());
	}

}
