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

package sample.jetty;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

@Configuration
@ComponentScan
public class SampleJettyApplication {

	private static final int SIZE = 1024 * 1024 * 500;

	private byte[] bytes = new byte[SIZE];

	@PostConstruct
	public void stuff() {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(4000);
					doStuff();
					System.out.println("Thread is done " + getName());
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
			};
		}.start();
	}

	@Bean
	public TickTickBean mybean() {
		return new TickTickBean();
	}

	protected void doStuff() throws Exception {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		while (classLoader != null) {
			System.out.println(ObjectUtils.getDisplayString(classLoader));
			System.out.println(classLoader.getClass());
			classLoader = classLoader.getParent();
		}
		System.out.println("Shutting it down");
		triggerShutdownHooks();
		cleanup();
		// Thread.sleep(1000000000000000000L);
		restart();
	}

	private void cleanup() throws Exception {
		Introspector.flushCaches();
		// Field field = Proxy.class.getDeclaredField("proxyClassCache");
		// field.setAccessible(true);
		// field.set(null, null);
	}

	private void restart() throws Exception {
		try {
			Class<?> forName = Thread.currentThread().getContextClassLoader().getParent()
					.loadClass("org.springframework.boot.loader.RestartableClassLoader");
			Method declaredMethod = forName.getDeclaredMethod("restart");
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			while (classLoader != null) {
				boolean instance = forName.isInstance(classLoader);
				System.out.println(classLoader + " is " + instance);
				if (instance) {
					System.out.println("Invoking " + declaredMethod);
					declaredMethod.invoke(classLoader);
					return;
				}
				classLoader = classLoader.getParent();
			}
		}
		catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			System.out.println("Can't restart");
		}
	}

	@SuppressWarnings("rawtypes")
	private void triggerShutdownHooks() throws Exception {
		Class<?> hooksClass = Class.forName("java.lang.ApplicationShutdownHooks");
		Method runHooks = hooksClass.getDeclaredMethod("runHooks");
		runHooks.setAccessible(true);
		runHooks.invoke(null);
		Field field = hooksClass.getDeclaredField("hooks");
		field.setAccessible(true);
		field.set(null, new IdentityHashMap());
		System.out.println("Shutdown done");
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Started main "
				+ SampleJettyApplication.class.getClassLoader());
		SpringApplication app = new SpringApplication(SampleJettyApplication.class);
		app.setWebEnvironment(false);
		app.run(args);
	}

}
