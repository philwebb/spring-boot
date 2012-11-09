package org.springframework.bootstrap.autoconfigure.example;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

@AutoConfiguration
@Conditional(SomeCondition.class)
public class ConfiguredByMe {

	@Bean
	public String myBean() {
		System.out.println("myBean");
		return "hello";
	}

}
