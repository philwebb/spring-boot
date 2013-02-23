package org.springframework.bootstrap.sample.jetty;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.AutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfiguration
public class JettyBootstrapApplication extends SpringApplication {

	public static void main(String[] args) throws Exception {
		new JettyBootstrapApplication().run(args);
	}

}
