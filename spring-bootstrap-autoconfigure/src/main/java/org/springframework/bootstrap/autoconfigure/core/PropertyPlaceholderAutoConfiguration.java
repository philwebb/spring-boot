
package org.springframework.bootstrap.autoconfigure.core;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@AutoConfiguration
@ConditionalOnMissingBean(PropertySourcesPlaceholderConfigurer.class)
public class PropertyPlaceholderAutoConfiguration {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}
