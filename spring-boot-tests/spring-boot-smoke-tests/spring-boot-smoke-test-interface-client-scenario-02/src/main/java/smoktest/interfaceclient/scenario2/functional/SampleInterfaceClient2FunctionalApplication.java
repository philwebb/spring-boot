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

package smoktest.interfaceclient.scenario2.functional;

import java.util.Map;

import smoktest.interfaceclient.scenario2.functional.SampleInterfaceClient2FunctionalApplication.Registrar;
import smoktest.interfaceclient.scenario2.generated.EchoService;

import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.client.support.RestClientHttpServiceProxyRegistry;
import org.springframework.web.service.registry.HttpServiceTypes;

@Configuration
@EnableAutoConfiguration
@Import(Registrar.class)
public class SampleInterfaceClient2FunctionalApplication {

	// Basic example with code generated interfaces (e.g. Open API).

	@Bean
	ApplicationRunner commandLineRunner(EchoService echoService) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario #2 (functional)");
			System.out.println();
			System.out.println(echoService.echo(Map.of("hello", "world")));
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleInterfaceClient2FunctionalApplication.class, args);
	}

	static class Registrar implements BeanRegistrar {

		@Override
		public void register(BeanRegistry registry, Environment env) {
			HttpServiceTypes serviceTypes = HttpServiceTypes.ofScan("smoktest.interfaceclient.scenario2.generated");
			new RestClientHttpServiceProxyRegistry(registry).registerBeans(serviceTypes,
					(serviceType, spec) -> spec.baseUrl("https://echo.zuplo.io"));
		}

	}

}
