package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.apache.catalina.startup.Tomcat;
import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedTomcatFactory;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({Servlet.class, Tomcat.class})
@ConditionalOnMissingBean(EmbeddedServletContainerFactory.class)
public class EmbeddedTomcatAutoConfiguration {

	@Bean
	public EmbeddedTomcatFactory embeddedTomcatFactory() {
		return new EmbeddedTomcatFactory();
	}

}
