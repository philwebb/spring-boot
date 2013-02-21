package org.springframework.bootstrap.autoconfigure;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public class AutoConfigureApplicationContextInitializerTests {

	@Test
	public void alwaysTheLastThingRegistered() throws Exception {
		//FIXME asserts
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class);
		AutoConfigurationSettings settings = mock(AutoConfigurationSettings.class);
		new AutoConfigurationApplicationContextInitializer(settings ).initialize(context);
		context.refresh();
	}


	@Configuration
	@Import(ImportedConfig.class)
	public static class Config {
		@Bean
		public ExampleBean bean1() {
			System.out.println("Bean1");
			return new ExampleBean();
		}
	}

	@Configuration
	//@DisableAutoConfiguration(ConfiguredByMe.class)
	public static class ImportedConfig {
		@Bean
		public ExampleBean bean2() {
			System.out.println("Bean2");
			return new ExampleBean();
		}
	}

	private static class ExampleBean {
	}
}
