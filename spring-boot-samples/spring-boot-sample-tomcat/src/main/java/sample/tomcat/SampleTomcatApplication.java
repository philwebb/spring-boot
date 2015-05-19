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

import java.util.Arrays;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

@SpringBootApplication
@Configuration
public class SampleTomcatApplication {

	private static Log logger = LogFactory.getLog(SampleTomcatApplication.class);

	private byte[] bytes = new byte[1024 * 100];

	// @Bean
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
					{
						setName("MyRestart");
					}

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

	// @Bean
	public TickTickBean tickBean() {
		return new TickTickBean();
	}

	public static void main(String[] args) {
		Restarter.initialize(args, true);
		SpringApplication.run(SampleTomcatApplication.class, args);
	}

	public static void xxxmain(String[] args) {
		SpringApplication app = new SpringApplication(SampleTomcatApplication.class);
		app.setWebEnvironment(false);
		app.run(args);
		System.out.println("cion " + Thread.currentThread().getContextClassLoader());
	}

	public static void xmain(String[] args) throws Exception {
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

	@SuppressWarnings("resource")
	public static void ymain(String[] args) throws Exception {
		Restarter.initialize(args);
		SampleTomcatApplication application = new SampleTomcatApplication();
		application.restartBean().afterPropertiesSet();
		final TickTickBean tickBean = application.tickBean();
		tickBean.afterPropertiesSet();
		tickBean.start();
		// SpringApplication app = new SpringApplication(Sometin.class);
		// app.setWebEnvironment(false);
		// app.run();

		// StaticApplicationContext context = new StaticApplicationContext();
		// ass(context);
		// context.refresh();

		GenericApplicationContext context = new GenericApplicationContext(); // AnnotationConfigApplicationContext();
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
		reader.register(Sometin.class);
		// context.register(Wibble.class);
		context.refresh();
		context.registerShutdownHook();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.gc();
				tickBean.stop();
				System.out.println("Bye from the shutdown hook");
			}
		});

	}

	/**
	 * @param context
	 */
	private static void ass(GenericApplicationContext context) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		context.registerBeanDefinition("test", beanDefinition);

		Set<BeanDefinitionHolder> set = AnnotationConfigUtils
				.registerAnnotationConfigProcessors(context, null);
		for (BeanDefinitionHolder beanDefinitionHolder : set) {
			String beanName = beanDefinitionHolder.getBeanName();
			System.out.println(beanName);

			String n1 = "org.springframework.context.annotation.internalConfigurationAnnotationProcessor";
			String n2 = "org.springframework.context.annotation.internalAutowiredAnnotationProcessor";
			String n3 = "org.springframework.context.annotation.internalRequiredAnnotationProcessor";
			String n4 = "org.springframework.context.annotation.internalCommonAnnotationProcessor";
			String n5 = "org.springframework.context.event.internalEventListenerProcessor";
			String n6 = "org.springframework.context.event.internalEventListenerFactory";
			if (!Arrays.asList(n3, n6).contains(beanName)) { // n3, n6
				// context.removeBeanDefinition(beanName);
			}
		}
	}

	@Configuration
	static class NoConfig {

	}

	@Configuration
	public static class Sometin {

		// @Bean
		public String hello() {
			return "Hello";
		}

	}

}
