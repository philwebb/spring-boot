package org.springframework.bootstrap.sample.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.sample.simple.service.HelloWorldService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class SimpleBootstrapApplication extends SpringApplication {

	// Simple example shows how a command line spring application can execute an
	// injected bean service. Also demonstraits how you can use @Value to inject
	// command line args ('--name=whatever')

	@Autowired
	private HelloWorldService helloWorldService;

	@Override
	protected void doRun(ApplicationConfigurationDetails configuration) {
		System.out.println(helloWorldService.getHelloMessage());
	}

	public static void main(String[] args) throws Exception {
		new SimpleBootstrapApplication().run(args);
	}
}
