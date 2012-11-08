
package org.springframework.tosort;

import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.web.context.EmbeddedServletProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Component
//@Conditional(NeverCondition.class)
public class JettyDunno implements EmbeddedServletProvider {

	@Value("#{ systemProperties['java.runtime.name'] }")
	private String defaultLocale;

	public void run() {
	}

	public void startEmbeddedServlet(WebApplicationContext applicationContext,
			ServletContextListener listener) throws Exception {
		System.out.println(defaultLocale);
		Server server = new Server(8080);
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(new ServletHolder(new DispatcherServlet(applicationContext)), "/*");
		context.addEventListener(listener);
		server.start();
	}


	// ContextLoader contextLoader = new ContextLoader(
	// (WebApplicationContext) applicationContext) {
	//
	// protected void configureAndRefreshWebApplicationContext(
	// org.springframework.web.context.ConfigurableWebApplicationContext wac,
	// javax.servlet.ServletContext sc) {
	// ApplicationContext parent = loadParentContext(sc);
	// wac.setParent(parent);
	// wac.setServletContext(sc);
	// customizeContext(sc, wac);
	// };
	// };
	// contextLoader.initWebApplicationContext(context.getServletContext());
}
