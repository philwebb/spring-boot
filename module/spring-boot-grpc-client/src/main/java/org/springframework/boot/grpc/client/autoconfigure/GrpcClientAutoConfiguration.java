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

import java.util.List;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientAutoConfiguration.GrpcClientScanConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.client.AbstractGrpcClientRegistrar;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.CoroutineStubFactory;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC clients.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(before = CompositeChannelFactoryAutoConfiguration.class)
@ConditionalOnGrpcClientEnabled
@EnableConfigurationProperties(GrpcClientProperties.class)
@Import({ GrpcCodecConfiguration.class, GrpcChannelFactoryConfigurations.ShadedNettyChannelFactoryConfiguration.class,
		GrpcChannelFactoryConfigurations.NettyChannelFactoryConfiguration.class,
		GrpcChannelFactoryConfigurations.InProcessChannelFactoryConfiguration.class,
		GrpcClientScanConfiguration.class })
public final class GrpcClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ClientInterceptorsConfigurer grpcClientInterceptorsConfigurer(ApplicationContext applicationContext) {
		return new ClientInterceptorsConfigurer(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(ChannelCredentialsProvider.class)
	PropertiesChannelCredentialsProvider grpcChannelCredentialsProvider(SslBundles bundles,
			GrpcClientProperties properties) {
		return new PropertiesChannelCredentialsProvider(properties, bundles);
	}

	@Bean
	<T extends ManagedChannelBuilder<T>> PropertiesGrpcChannelBuilderCustomizer<T> grpcClientPropertiesChannelCustomizer(
			GrpcClientProperties properties) {
		return new PropertiesGrpcChannelBuilderCustomizer<>(properties);
	}

	@Bean
	<T extends ManagedChannelBuilder<T>> DefaultServicesChannelBuilderCustomizer<T> grpcDefaultServicesChannelBuilderCustomizer(
			GrpcClientProperties properties,
			ObjectProvider<GrpcChannelBuilderDefaultServiceConfigCustomizer> customizers) {
		return new DefaultServicesChannelBuilderCustomizer<>(properties, customizers);
	}

	@Bean
	@Order(0)
	@ConditionalOnBean(CompressorRegistry.class)
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> grpcCompressionChannelBuilderCustomizer(
			CompressorRegistry registry) {
		return (name, builder) -> builder.compressorRegistry(registry);
	}

	@Bean
	@Order(0)
	@ConditionalOnBean(DecompressorRegistry.class)
	<T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> grpcDecompressionChannelBuilderCustomizer(
			DecompressorRegistry registry) {
		return (name, builder) -> builder.decompressorRegistry(registry);
	}

	@Bean
	ChannelBuilderCustomizers grpChannelBuilderCustomizers(
			ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers) {
		return new ChannelBuilderCustomizers(customizers);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "io.grpc.kotlin.AbstractCoroutineStub")
	static class GrpcClientCoroutineStubConfiguration {

		@Bean
		@ConditionalOnMissingBean
		CoroutineStubFactory coroutineStubFactory() {
			return new CoroutineStubFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(GrpcClientFactory.class)
	@Import(ClientRegistrar.class)
	static class GrpcClientScanConfiguration {

	}

	/**
	 * {@link AbstractGrpcClientRegistrar} for auto-configured clients.
	 */
	static class ClientRegistrar extends AbstractGrpcClientRegistrar {

		private static final GrpcClientRegistrationSpec[] NO_REGISTRATIONS = {};

		private final Environment environment;

		private final BeanFactory beanFactory;

		ClientRegistrar(Environment environment, BeanFactory beanFactory) {
			this.environment = environment;
			this.beanFactory = beanFactory;
		}

		@Override
		protected GrpcClientRegistrationSpec[] collect(AnnotationMetadata metadata) {
			if (!AutoConfigurationPackages.has(this.beanFactory)) {
				return NO_REGISTRATIONS;
			}
			GrpcClientProperties.Autoconfigure grpcAutoconfigureProperties = Binder.get(this.environment)
				.bind("spring.grpc.client.autoconfigure", GrpcClientProperties.Autoconfigure.class)
				.orElse(null);
			List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
			GrpcClientRegistrationSpec spec = GrpcClientRegistrationSpec.of("default")
				.factory((grpcAutoconfigureProperties != null) ? grpcAutoconfigureProperties.getStubFactory() : null)
				.packages(packages.toArray(String[]::new));
			return new GrpcClientRegistrationSpec[] { spec };
		}

	}

}
