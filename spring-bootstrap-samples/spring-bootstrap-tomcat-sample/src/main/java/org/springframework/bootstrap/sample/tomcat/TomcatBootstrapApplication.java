package org.springframework.bootstrap.sample.tomcat;

import org.springframework.bootstrap.SpringApplication;

public class TomcatBootstrapApplication extends SpringApplication{

	public static void main(String[] args) throws Exception {
		new TomcatBootstrapApplication().run(args);
	}

}
