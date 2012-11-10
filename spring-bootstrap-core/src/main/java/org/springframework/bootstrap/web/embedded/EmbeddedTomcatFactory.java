package org.springframework.bootstrap.web.embedded;

import java.io.File;

import javax.servlet.ServletContextListener;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class EmbeddedTomcatFactory implements EmbeddedServletContainerFactory {

	public EmbeddedServletContainer getContainer(
			WebApplicationContext applicationContext, ServletContextListener listener)
			throws Exception {

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);

		StandardContext context = (StandardContext) tomcat.addContext("", new File(".").getAbsolutePath());
		context.addApplicationLifecycleListener(listener);
		Tomcat.addServlet(context, "Spring", new DispatcherServlet(applicationContext)).setLoadOnStartup(1);
		context.addServletMapping("/*", "Spring");

		return new TomcatEmbeddedServletContainer(tomcat);
	}

	private static class TomcatEmbeddedServletContainer implements EmbeddedServletContainer {

		private Tomcat tomcat;

		public TomcatEmbeddedServletContainer(Tomcat tomcat) {
			this.tomcat = tomcat;
		}

		public void start() throws Exception {
			tomcat.start();
			new Thread() {
				public void run() {
					//FIXME probably should not be a thread
					tomcat.getServer().await();
				};
			}.start();
		}

		public void stop() throws Exception {
			tomcat.stop();
		}
	}


}
