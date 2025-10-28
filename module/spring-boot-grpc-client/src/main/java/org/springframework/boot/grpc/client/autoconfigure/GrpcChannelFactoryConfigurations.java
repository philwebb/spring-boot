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

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorFilter;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

/**
 * Configurations for {@link GrpcChannelFactory gRPC channel factories}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcChannelFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ io.grpc.netty.shaded.io.netty.channel.Channel.class,
			io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class })
	@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(name = "spring.grpc.client.inprocess.exclusive", havingValue = "false",
			matchIfMissing = true)
	static class ShadedNettyChannelFactoryConfiguration {

		@Bean
		ShadedNettyGrpcChannelFactory shadedNettyGrpcChannelFactory(Environment environment,
				GrpcClientProperties properties, ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
				ChannelCredentialsProvider credentials) {
			ShadedNettyGrpcChannelFactory factory = new ShadedNettyGrpcChannelFactory(
					channelBuilderCustomizers.forFactory(), interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(new PropertiesVirtualTargets(environment, properties));
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Channel.class, NettyChannelBuilder.class })
	@ConditionalOnMissingBean(value = GrpcChannelFactory.class, ignored = InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(name = "spring.grpc.client.inprocess.exclusive", havingValue = "false",
			matchIfMissing = true)
	static class NettyChannelFactoryConfiguration {

		@Bean
		NettyGrpcChannelFactory nettyGrpcChannelFactory(Environment environment, GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers,
				ChannelCredentialsProvider credentials) {
			NettyGrpcChannelFactory factory = new NettyGrpcChannelFactory(channelBuilderCustomizers.forFactory(),
					interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(new PropertiesVirtualTargets(environment, properties));
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InProcessChannelBuilder.class)
	@ConditionalOnMissingBean(InProcessGrpcChannelFactory.class)
	@ConditionalOnProperty(name = "spring.grpc.client.inprocess.enabled", havingValue = "true", matchIfMissing = true)
	static class InProcessChannelFactoryConfiguration {

		@Bean
		InProcessGrpcChannelFactory inProcessGrpcChannelFactory(ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer,
				ObjectProvider<ClientInterceptorFilter> interceptorFilter,
				ObjectProvider<GrpcChannelFactoryCustomizer> channelFactoryCustomizers) {
			InProcessGrpcChannelFactory factory = new InProcessGrpcChannelFactory(
					channelBuilderCustomizers.forFactory(), interceptorsConfigurer);
			interceptorFilter.ifAvailable(factory::setInterceptorFilter);
			channelFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			return factory;
		}

	}

}
