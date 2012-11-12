
package org.springframework.bootstrap.web.embedded;

import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create {@link EmbeddedServletContainer}s.
 * @author Phillip webb
 */
public interface EmbeddedServletContainerFactory {

	public EmbeddedServletContainer getContainer(
			WebApplicationContext applicationContext, ServletContextListener listener)
			throws Exception;

}
