package org.springframework.bootstrapsample;

import org.springframework.stereotype.Component;

@Component
public class MyBean {

	public String sayHello() {
		return "Hello";
	}

}
