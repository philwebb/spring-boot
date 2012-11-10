package org.springframework.bootstrapsample;

import org.springframework.bootstrap.web.embedded.EmbeddedJettyFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
public class SampleConfiguration {

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
