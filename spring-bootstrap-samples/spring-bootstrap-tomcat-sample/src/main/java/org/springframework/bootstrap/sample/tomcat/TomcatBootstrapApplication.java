package org.springframework.bootstrap.sample.tomcat;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class TomcatBootstrapApplication extends SpringApplication{

	public static void main(String[] args) throws Exception {
		new TomcatBootstrapApplication().run(args);
	}

}
