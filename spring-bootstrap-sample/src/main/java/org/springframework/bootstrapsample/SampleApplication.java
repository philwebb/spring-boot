package org.springframework.bootstrapsample;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.tosort.JettyDunno;


public class SampleApplication extends SpringApplication{

	@Override
	protected Class<?> getRunBean() {
		return JettyDunno.class;
	}

	public static void main(String[] args) throws Exception {
		new SampleApplication().run(args);
	}

}
