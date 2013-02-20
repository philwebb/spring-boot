/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.bootstrap.web.embedded;

import java.io.File;

import javax.servlet.ServletContextListener;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EmbeddedServletContainerFactory} for Tomcat.
 *
 * @author Phillip Webb
 */
public class EmbeddedTomcatFactory implements EmbeddedServletContainerFactory {

	// FIXME see notes in Jetty factory

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
