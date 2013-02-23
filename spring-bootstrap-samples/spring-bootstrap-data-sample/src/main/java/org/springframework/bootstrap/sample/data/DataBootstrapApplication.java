package org.springframework.bootstrap.sample.data;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class DataBootstrapApplication extends SpringApplication{

	public static void main(String[] args) throws Exception {
		new DataBootstrapApplication().run(args);
	}

}
