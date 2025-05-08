/*
 * Copyright 2012-2025 the original author or authors.
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

package smoktest.interfaceclient.scenario6.programmatic;

import java.util.Locale;
import java.util.Map;

import smoktest.interfaceclient.scenario6.programmatic.SampleInterfaceClient6ProgrammaticApplication.Registrar;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup;

@Configuration
@EnableAutoConfiguration
@Import(Registrar.class)
public class SampleInterfaceClient6ProgrammaticApplication {

	// Basic example that also sets adds custom argument resolver via proxy factory.

	@Bean
	ApplicationRunner commandLineRunner(EchoService echoService) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario 6 (funcational)");
			System.out.println();
			System.out.println(echoService.echo(new EchoArgument("hello", "world")));
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	@Bean
	RestClientHttpServiceGroupConfigurer httpServiceGroupConfigurer() {
		return (groups) -> groups.configureProxyFactory(this::configureProxyFactory);
	}

	private void configureProxyFactory(HttpServiceGroup group, HttpServiceProxyFactory.Builder builder) {
		builder.customArgumentResolver(this::resolve);
	}

	private boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
		if (argument instanceof EchoArgument echoArgument) {
			requestValues.setBodyValue(Map.of(echoArgument.hello(), echoArgument.world().toUpperCase(Locale.ROOT)));
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleInterfaceClient6ProgrammaticApplication.class, args);
	}

	static class Registrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {
			registry.forGroup("zuplo").register(EchoService.class);
		}

	}

}
