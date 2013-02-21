package org.springframework.bootstrap.sample.data;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.autoconfigure.orm.jpa.JpaAutoConfiguration;

public class DataBootstrapApplication extends SpringApplication{

	@Override
	protected void configure(Configuration configuration) {
		configuration.configure(JpaAutoConfiguration.class).setDunno("");
	}

	public static void main(String[] args) throws Exception {
		new DataBootstrapApplication().run(args);
	}

}
