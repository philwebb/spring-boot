
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;

public interface EmbeddedServletContainerFactory {

	public EmbeddedServletContainer getContainer(
			WebApplicationContext applicationContext, ServletContextListener listener)
			throws Exception;

}
