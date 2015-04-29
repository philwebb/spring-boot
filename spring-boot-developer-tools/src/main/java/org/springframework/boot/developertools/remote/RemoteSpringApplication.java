/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.developertools.remote;

import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Phillip Webb
 */
@Configuration
@Import({ RemoteDebugHttpTunnelClientConfiguration.class })
public class RemoteSpringApplication {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(
				RemoteSpringApplication.class);
		application.setWebEnvironment(false);
		ClassPathResource banner = new ClassPathResource("banner.txt",
				RemoteSpringApplication.class);
		application.setBanner(new ResourceBanner(banner));
		application.addListeners(new RemoteUrlPropertyExtractor());
		application.run(args);
		waitIndefinitely();
	}

	private static void waitIndefinitely() {
		while (true) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ex) {
			}
		}
	}

}
