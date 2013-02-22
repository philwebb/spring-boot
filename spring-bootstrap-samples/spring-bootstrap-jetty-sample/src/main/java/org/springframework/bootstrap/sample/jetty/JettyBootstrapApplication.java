package org.springframework.bootstrap.sample.jetty;

import org.springframework.bootstrap.AutoConfigureSpringApplication;
import org.springframework.bootstrap.context.annotation.AutoConfiguration;

@AutoConfiguration
public class JettyBootstrapApplication extends AutoConfigureSpringApplication {

	public static void main(String[] args) throws Exception {
		new JettyBootstrapApplication().run(args);
	}

}
