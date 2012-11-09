package org.springframework.bootstrapsample.web;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExampleCommandLineArgs {

	@Value("${port:8080}")
	private int port;

	@PostConstruct
	public void print() {
		System.out.println("****** The port is : " + port);
	}

}
