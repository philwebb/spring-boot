/*
 * Copyright 2012-2013 the original author or authors.
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

package sample.tomcat;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.annotation.Bean;

//@SpringBootApplication
public class SampleTomcatApplication {

	private static Log logger = LogFactory.getLog(SampleTomcatApplication.class);

	private byte[] bytes = new byte[1024 * 100];

	@Bean
	protected ServletContextListener listener() {
		return new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				logger.info("ServletContext initialized");
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				logger.info("ServletContext destroyed");
			}

		};
	}

	@Bean
	public InitializingBean restartBean() {
		return new InitializingBean() {

			@Override
			public void afterPropertiesSet() throws Exception {
				Thread thread = new Thread() {
					@Override
					public void run() {
						try {
							System.gc();
							Thread.sleep(2000);
							Restarter.getInstance().restart();
							System.out.println("Bye from the restarter");
						}
						catch (InterruptedException ex) {
						}
					};
				};
				thread.start();
			}

		};
	}

	@Bean
	public TickTickBean tickBean() {
		return new TickTickBean();
	}

	public static void main(String[] args) throws Exception {
		Restarter.initialize(args);
		SampleTomcatApplication application = new SampleTomcatApplication();
		application.restartBean().afterPropertiesSet();
		final TickTickBean tickBean = application.tickBean();
		tickBean.afterPropertiesSet();
		tickBean.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.gc();
				tickBean.stop();
				System.out.println("Bye from the shutdown hook");
			}
		});
	}
}
