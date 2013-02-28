package org.springframework.bootstrap.sample.data;

import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StopWatch;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class DataBootstrapApplication extends SpringApplication implements ApplicationListener<ContextRefreshedEvent> {

	private static StopWatch t;

	public DataBootstrapApplication() {
		t = new StopWatch("Application");
		t.start();
	}

	public static void main(String[] args) throws Exception {
		new DataBootstrapApplication().run(args);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		t.stop();
		System.out.println(t.prettyPrint());
	}

}
