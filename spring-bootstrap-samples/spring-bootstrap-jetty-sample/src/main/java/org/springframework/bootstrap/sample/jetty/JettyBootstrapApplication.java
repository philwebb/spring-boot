package org.springframework.bootstrap.sample.jetty;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class JettyBootstrapApplication extends SpringApplication {

	@Override
	protected void configure(Configuration configuration) {
		configuration.disableAutoConfigure();
	}

	public static void main(String[] args) throws Exception {
		new JettyBootstrapApplication().run(args);
	}

}
