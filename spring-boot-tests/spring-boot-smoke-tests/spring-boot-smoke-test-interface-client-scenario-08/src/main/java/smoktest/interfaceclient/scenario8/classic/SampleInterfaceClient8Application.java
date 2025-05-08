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

package smoktest.interfaceclient.scenario8.classic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@ImportHttpServices(group = "zuplo1", types = EchoService.class)
@ImportHttpServices(group = "zuplo2", types = EchoService.class)
public class SampleInterfaceClient8Application {

	// The same interface for the same host, but with different client setup.

	@Bean
	ApplicationRunner commandLineRunner(List<EchoService> echoServices) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario #8 (classic)");
			System.out.println();
			for (EchoService echoService : echoServices) {
				System.out.println(echoService.echo(Map.of("hello", "world")));
			}
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	@Bean
	ApplicationRunner commandLineRunnerByName(HttpServiceProxyRegistry registry) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario #8 (classic by name)");
			System.out.println();
			System.out.println(registry.getClient("zuplo1", EchoService.class).echo(Map.of("hello", "world")));
			System.out.println(registry.getClient("zuplo2", EchoService.class).echo(Map.of("hello", "universe")));
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	// FIXME @Bean
	ApplicationRunner commandLineRunnerByQualifier(@Qualifier("zuplo1") EchoService echoService) {
		return (args) -> {
			System.out.println();
			System.out.println();
			System.out.println("==========================");
			System.out.println("Scenario #8 (classic by name)");
			System.out.println();
			System.out.println(echoService.echo(Map.of("hello", "world")));
			System.out.println("==========================");
			System.out.println();
			System.out.println();
		};
	}

	@Bean
	RestClientHttpServiceGroupConfigurer uppercaseRestClientHttpServiceGroupConfigurer() {
		return (groups) -> groups.configureClient(this::configureClient);
	}

	private void configureClient(HttpServiceGroup group, RestClient.Builder builder) {
		if ("zuplo1".equals(group.name())) {
			builder.requestInterceptor(this::intercept);
		}
	}

	ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		return execution.execute(request,
				new String(body, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleInterfaceClient8Application.class, args);
	}

}
