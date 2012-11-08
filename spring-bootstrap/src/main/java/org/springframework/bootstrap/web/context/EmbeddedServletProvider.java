
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContextListener;

import org.springframework.web.context.WebApplicationContext;

public interface EmbeddedServletProvider {

	public void startEmbeddedServlet(WebApplicationContext applicationContext, ServletContextListener listener)
			throws Exception;

}
