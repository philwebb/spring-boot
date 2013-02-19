package org.springframework.bootstrapsample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.context.ApplicationContext;

public class SimpleSampleApplication extends SpringApplication{

	@Autowired
	private MyBean myBean;

	@Override
	protected void doRun(ConfigurationDetails configuration,
			ApplicationContext applicationContext) {
		System.out.println(myBean.sayHello() + " World");
	}

	public static void main(String[] args) throws Exception {
		new SimpleSampleApplication().run(args);
	}

}
