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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

public class ReloadingSampleTomcatApplication {

	private static Log logger = LogFactory.getLog(ReloadingSampleTomcatApplication.class);

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

	public static void main(String[] args) throws Exception {
		URLClassLoader classLoader = (URLClassLoader) Thread.currentThread()
				.getContextClassLoader();
		URL appUrl = null;
		List<URL> urls = new ArrayList<URL>(Arrays.asList(classLoader.getURLs()));
		for (Iterator<URL> iterator = urls.iterator(); iterator.hasNext();) {
			URL url = iterator.next();
			if (url.toString().contains("sample-tomcat")) {
				appUrl = url;
				iterator.remove();
			}
		}
		ClassLoader root = classLoader.getParent();
		URLClassLoader parentClassLoader = new URLClassLoader(urls.toArray(new URL[urls
				.size()]), root);
		boolean cont = true;
		while (cont) {
			Thread runner = makeRunner(appUrl, parentClassLoader, args);
			System.out.println("Go");
			runner.start();
			Thread.sleep(10000);
			System.out.println("Shutup");
			runShutdownHooks();
			System.out.println("Made the all shutup, reload");
		}
	}

	/**
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchFieldException
	 */
	private static void runShutdownHooks() throws ClassNotFoundException,
			IllegalAccessException, InvocationTargetException, NoSuchFieldException {
		Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
		Method runHooksMethod = ReflectionUtils.findMethod(hooksClass, "runHooks");
		runHooksMethod.setAccessible(true);
		runHooksMethod.invoke(null);
		Field hooksField = hooksClass.getDeclaredField("hooks");
		hooksField.setAccessible(true);
		hooksField.set(null, new IdentityHashMap());
	}

	private static Thread makeRunner(URL appUrl, URLClassLoader parentClassLoader,
			final String[] args) {
		final URLClassLoader childClassLoader = new URLClassLoader(new URL[] { appUrl },
				parentClassLoader);
		Thread runner = new Thread() {
			@Override
			public void run() {
				try {
					ClassLoader runCL = childClassLoader;
					System.out.println(runCL);
					Class<?> appClass = ClassUtils.forName(
							"sample.tomcat.SampleTomcatApplication", runCL);
					System.out.println(appClass.getClassLoader());
					Method mainMethod = ReflectionUtils.findMethod(appClass, "main",
							String[].class);
					mainMethod.invoke(null, new Object[] { args });
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		runner.setContextClassLoader(childClassLoader);
		return runner;
	}

}
