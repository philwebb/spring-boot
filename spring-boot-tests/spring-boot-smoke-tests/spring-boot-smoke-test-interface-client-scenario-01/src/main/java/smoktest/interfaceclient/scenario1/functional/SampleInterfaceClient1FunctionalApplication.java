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

package smoktest.interfaceclient.scenario1.functional;

import java.util.Map;

import smoktest.interfaceclient.scenario1.functional.SampleInterfaceClient1FunctionalApplication.Registrar;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;

@Configuration
@EnableAutoConfiguration
@Import(Registrar.class)
public class SampleInterfaceClient1FunctionalApplication {

	// Basic example with only the baseUrl set.

	@Bean
	ApplicationRunner commandLineRunner(EchoServiceClient echoService) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario #1 (functional)");
			System.out.println();
			System.out.println(echoService.echo(Map.of("hello", "world")));
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleInterfaceClient1FunctionalApplication.class, args);
	}

	static class Registrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata importingClassMetadata) {
			registry.forDefaultGroup().register(EchoServiceClient.class);
		}

	}

}
