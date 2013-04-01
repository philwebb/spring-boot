package org.springframework.bootstrap;


/**
 * @author Dave Syer
 */
public interface CommandLineRunner {

	//FIXME replace with @Run

	void run(String... args);

}
