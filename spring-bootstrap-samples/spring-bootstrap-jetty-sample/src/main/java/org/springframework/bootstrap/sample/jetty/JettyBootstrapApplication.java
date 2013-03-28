package org.springframework.bootstrap.sample.jetty;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class JettyBootstrapApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.main(JettyBootstrapApplication.class, args);
	}
	
}
