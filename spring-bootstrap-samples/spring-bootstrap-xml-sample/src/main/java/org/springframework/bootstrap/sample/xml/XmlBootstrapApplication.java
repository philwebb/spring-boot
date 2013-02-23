package org.springframework.bootstrap.sample.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.sample.xml.service.HelloWorldService;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class XmlBootstrapApplication extends SpringApplication {

	@Autowired
	private HelloWorldService helloWorldService;

	@Override
	protected void configure(ApplicationConfiguration configuration) {
		configuration.addImport("classpath:/META-INF/application-context.xml");
	}

	@Override
	protected void doRun(ApplicationConfigurationDetails configuration) {
		System.out.println(helloWorldService.getHelloMessage());
	}

	public static void main(String[] args) throws Exception {
		new XmlBootstrapApplication().run(args);
	}
}
