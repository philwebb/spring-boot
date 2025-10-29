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

import io.grpc.BindableService;
import io.grpc.servlet.jakarta.GrpcServlet;
import io.grpc.servlet.jakarta.ServletServerBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogMessage;
import org.springframework.grpc.server.service.GrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server factories.
 * <p>
 * gRPC must be on the classpath and at least one {@link BindableService} bean registered
 * in the context in order for the auto-configuration to execute.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Toshiaki Maki
 * @since 4.0.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnSpringGrpc
@ConditionalOnGrpcServerEnabled
@ConditionalOnBean(BindableService.class)
public final class GrpcServerFactoryAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnGrpcNativeServer
	static class GrpcServerFactoryConfiguration {

		// FIXME double nested
		@Configuration(proxyBeanMethods = false)
		@Import({ GrpcServerFactoryConfigurations.ShadedNettyServerFactoryConfiguration.class,
				GrpcServerFactoryConfigurations.NettyServerFactoryConfiguration.class,
				GrpcServerFactoryConfigurations.InProcessServerFactoryConfiguration.class })
		static class NettyServerFactoryConfiguration {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnGrpcServletServer
	public static class GrpcServletConfiguration {

		private static Log logger = LogFactory.getLog(GrpcServletConfiguration.class);

		// FIXME need PropertiesServerBuilderCustomizer

		@Bean
		ServletRegistrationBean<GrpcServlet> grpcServlet(GrpcServerProperties properties,
				GrpcServiceDiscoverer serviceDiscoverer, GrpcServiceConfigurer serviceConfigurer,
				ServerBuilderCustomizers serverBuilderCustomizers) {
			GrpcServices services = new GrpcServices(serviceDiscoverer, serviceConfigurer);
			services.names().forEach(this::logRegisteringServiceMessage);
			ServletServerBuilder builder = new ServletServerBuilder();
			serviceDiscoverer.findServices()
				.stream()
				.map((serviceSpec) -> serviceConfigurer.configure(serviceSpec, null))
				.forEach(builder::addService);
			serverBuilderCustomizers.customize(builder);
			GrpcServlet servlet = builder.buildServlet();
			ServletRegistrationBean<GrpcServlet> registration = new ServletRegistrationBean<>(servlet);
			registration.setUrlMappings(services.names().map(this::servicePath).toList());
			return registration;
		}

		private void logRegisteringServiceMessage(String serviceName) {
			logger.info(LogMessage.format("Registering gRPC service: %s", serviceName));
		}

		private String servicePath(String serviceName) {
			return "/" + serviceName + "/*";
		}

		@Configuration(proxyBeanMethods = false)
		@Import(GrpcServerFactoryConfigurations.InProcessServerFactoryConfiguration.class)
		static class InProcessConfiguration {

			// FIXME Why nest?

		}

	}

}
