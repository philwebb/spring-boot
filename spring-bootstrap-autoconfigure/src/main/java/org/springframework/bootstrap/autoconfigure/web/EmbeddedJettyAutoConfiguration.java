package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.mortbay.jetty.Server;
import org.mortbay.util.Loader;
import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.bootstrap.web.embedded.EmbeddedJettyFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({Servlet.class, Server.class, Loader.class})
@ConditionalOnMissingBean(EmbeddedServletContainerFactory.class)
public class EmbeddedJettyAutoConfiguration {

	@Bean
	public EmbeddedJettyFactory embeddedJettyFactory() {
		return new EmbeddedJettyFactory();
	}

}
