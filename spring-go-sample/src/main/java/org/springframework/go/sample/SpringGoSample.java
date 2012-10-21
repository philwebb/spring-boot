package org.springframework.go.sample;

import org.springframework.go.WebApplication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;


public class SpringGoSample extends WebApplication {

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		return null;
	}

	@Override
	protected WebApplicationContext createServletApplicationContext() {
		return new AnnotationConfigWebApplicationContext();
	}

	/*
	 * This class would be all that is required to configure a Spring-Go Classic
	 * we application.  None of the methods would need to be implemented.
	 *
	 * The class would mark a classpath scanning base package.  All classes below
	 * this will be scanned.
	 *
	 * Features will be intelligently enabled depending on what is on the classpath
	 * in combination with what @Configuration is already enabled.  For example
	 * if Hibernate is on the classpath a local LocalEntityManagerFactoryBean
	 * will be configured to scan for all @Entities.  Likewise with items such
	 * as SpringData, Mongo, Web Renderers.
	 */

}
