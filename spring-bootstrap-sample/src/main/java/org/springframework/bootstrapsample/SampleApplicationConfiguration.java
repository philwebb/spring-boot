package org.springframework.bootstrapsample;

import org.springframework.bootstrap.web.embedded.EmbeddedJettyFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class SampleApplicationConfiguration {

	@Bean
	public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		// we should enable by default
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public EmbeddedServletContainerFactory embeddedServletContainer() {
		return new EmbeddedJettyFactory();
	}
}
