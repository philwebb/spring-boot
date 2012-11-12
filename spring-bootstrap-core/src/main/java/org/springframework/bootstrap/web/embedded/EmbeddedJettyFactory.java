
package org.springframework.bootstrap.web.embedded;

import javax.servlet.ServletContextListener;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EmbeddedServletContainerFactory} for Jetty.
 * @author Phillip Webb
 */
public class EmbeddedJettyFactory implements EmbeddedServletContainerFactory {

	// FIXME we should define common properties for jetty/tomcat eg port
	// FIXME how to specify servlet

	public EmbeddedServletContainer getContainer(
			WebApplicationContext applicationContext, ServletContextListener listener)
			throws Exception {
		final Server server = new Server(8080);
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(new ServletHolder(new DispatcherServlet(applicationContext)), "/*");
		context.addEventListener(listener);
		return new EmbeddedJettyServer(server);
	}

	private static class EmbeddedJettyServer implements EmbeddedServletContainer {

		private Server server;

		public EmbeddedJettyServer(Server server) {
			this.server = server;
		}

		public void start() throws Exception {
			server.start();
		}

		public void stop() throws Exception {
			server.stop();
		}
	}
}
