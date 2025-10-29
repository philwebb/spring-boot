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

import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerServiceDefinitionFilter;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.grpc.server.service.ServerInterceptorFilter;

/**
 * Configurations for {@link GrpcServerFactory gRPC server factories}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcServerFactoryConfigurations {

	// FIXME create PropertiesServerBuilderCustomizer bean

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)
	@ConditionalOnMissingBean(value = GrpcServerFactory.class, ignored = InProcessGrpcServerFactory.class)
	@ConditionalOnProperty(name = "spring.grpc.server.inprocess.exclusive", havingValue = "false",
			matchIfMissing = true)
	static class ShadedNettyServerFactoryConfiguration {

		@Bean
		ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers, SslBundles bundles,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			GrpcServices services = new GrpcServices(serviceDiscoverer, serviceConfigurer);
			ServerCredentials serverCredentials = ServerCredentials.get(properties, bundles,
					io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE);
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
					serverBuilderCustomizers.forFactory(), serverCredentials.keyManager(),
					serverCredentials.trustManager(), serverCredentials.clientAuth());
			customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			services.addToServerFactory(factory);
			return factory;
		}

		@ConditionalOnBean(ShadedNettyGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "shadedNettyGrpcServerLifecycle")
		@Bean
		GrpcServerLifecycle shadedNettyGrpcServerLifecycle(ShadedNettyGrpcServerFactory factory,
				GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyServerBuilder.class)
	@ConditionalOnMissingBean(value = GrpcServerFactory.class, ignored = InProcessGrpcServerFactory.class)
	@ConditionalOnProperty(name = "spring.grpc.server.inprocess.exclusive", havingValue = "false",
			matchIfMissing = true)
	static class NettyServerFactoryConfiguration {

		@Bean
		NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers, SslBundles bundles,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			GrpcServices services = new GrpcServices(serviceDiscoverer, serviceConfigurer);
			ServerCredentials serverCredentials = ServerCredentials.get(properties, bundles,
					InsecureTrustManagerFactory.INSTANCE);
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(),
					serverBuilderCustomizers.forFactory(), serverCredentials.keyManager(),
					serverCredentials.trustManager(), serverCredentials.clientAuth());
			customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			services.addToServerFactory(factory);
			return factory;
		}

		@ConditionalOnBean(NettyGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "nettyGrpcServerLifecycle")
		@Bean
		GrpcServerLifecycle nettyGrpcServerLifecycle(NettyGrpcServerFactory factory, GrpcServerProperties properties,
				ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InProcessGrpcServerFactory.class)
	@ConditionalOnMissingBean(InProcessGrpcServerFactory.class)
	@ConditionalOnProperty("spring.grpc.server.inprocess.name")
	static class InProcessServerFactoryConfiguration {

		@Bean
		InProcessGrpcServerFactory inProcessGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers,
				ObjectProvider<ServerInterceptorFilter> interceptorFilter,
				ObjectProvider<ServerServiceDefinitionFilter> serviceFilter,
				ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
			GrpcServices services = new GrpcServices(serviceDiscoverer, serviceConfigurer);
			InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(properties.getInprocess().getName(),
					serverBuilderCustomizers.forFactory());
			factory.setInterceptorFilter(interceptorFilter.getIfAvailable());
			factory.setServiceFilter(serviceFilter.getIfAvailable());
			customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
			services.addToServerFactory(factory);
			return factory;
		}

		@Bean
		@ConditionalOnBean(InProcessGrpcServerFactory.class)
		@ConditionalOnMissingBean(name = "inProcessGrpcServerLifecycle")
		GrpcServerLifecycle inProcessGrpcServerLifecycle(InProcessGrpcServerFactory factory,
				GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
		}

	}

}
