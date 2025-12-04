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

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(NettyServerBuilder.class)
@ConditionalOnMissingGrpcServer
@ConditionalOnProperty(name = "spring.grpc.server.inprocess.exclusive", havingValue = "false", matchIfMissing = true)
class GrpcShadedNettyServerConfiguration {

	@Bean
	ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
			GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
			ServerBuilderCustomizers serverBuilderCustomizers, SslBundles bundles,
			ObjectProvider<GrpcServerFactoryCustomizer> customizers) {
		GrpcServices services = new GrpcServices(serviceDiscoverer, serviceConfigurer);
		ServerCredentials serverCredentials = ServerCredentials.get(properties, bundles,
				InsecureTrustManagerFactory.INSTANCE);
		ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
				serverBuilderCustomizers.forFactory(), serverCredentials.keyManager(), serverCredentials.trustManager(),
				serverCredentials.clientAuth());
		customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
		services.addToServerFactory(factory);
		return factory;
	}

	// FIXME make common? Perhaps auto-register lifecycle if there isn't one?
	@Bean
	@ConditionalOnMissingBean(name = "shadedNettyGrpcServerLifecycle")
	GrpcServerLifecycle shadedNettyGrpcServerLifecycle(ShadedNettyGrpcServerFactory factory,
			GrpcServerProperties properties, ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, properties.getShutdown().getGracePeriod(), eventPublisher);
	}

}