package org.springframework.bootstrap.web.embedded.api2;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author pwebb
 */
public class EmbeddedDispatcherServletInitializer implements ApplicationContextAware, EmbeddedServletContainerInitializer {

// Model on AbstractDispatcherServletInitializer

	@Override
	public void onStartup(ServletContext context) {
		// TODO Auto-generated method stub
		// Create the dispatcher servlet
		// etc
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		// TODO Auto-generated method stub

	}

}
