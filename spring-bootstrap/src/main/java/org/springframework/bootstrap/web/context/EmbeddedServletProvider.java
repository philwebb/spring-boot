
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

public interface EmbeddedServletProvider {

	public ServletContext startEmbeddedServlet(WebApplicationContext applicationContext)
			throws Exception;

}
