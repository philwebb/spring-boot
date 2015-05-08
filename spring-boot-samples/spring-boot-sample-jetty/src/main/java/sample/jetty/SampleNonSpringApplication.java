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
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

public class SampleNonSpringApplication {

	private static final int SIZE = 1024 * 1024 * 500;

	private byte[] bytes = new byte[SIZE];

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
		System.out.println("Removing the damn cache");
		// purgeCache(ResolvableType.class.getDeclaredField("cache"));
		// purgeCache(ReflectionUtils.class.getDeclaredField("declaredMethodsCache"));
		// purgeCache(ReflectionUtils.class.getDeclaredField("declaredFieldsCache"));

		Introspector.flushCaches();

		// Field field = Proxy.class.getDeclaredField("proxyClassCache");
		// field.setAccessible(true);
		// field.set(null, null);
	}

	/**
	 * @param field
	 * @throws IllegalAccessException
	 */
	private void purgeCache(Field field) throws IllegalAccessException {
		field.setAccessible(true);
		((ConcurrentReferenceHashMap) (field.get(null))).clear();
		((ConcurrentReferenceHashMap) (field.get(null))).purgeUnreferencedEntries();
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

	public static void xmain(String[] args) throws Exception {
		SampleNonSpringApplication application = new SampleNonSpringApplication();

		ReflectionUtils
				.getUniqueDeclaredMethods(AnnotationConfigApplicationContext.class);
		// StaticApplicationContext context = new StaticApplicationContext();
		// context.refresh();
		// context.registerShutdownHook();

		GenericApplicationContext context = new GenericApplicationContext(); // AnnotationConfigApplicationContext();
		// ConfigurationClassPostProcessor processor = new
		// ConfigurationClassPostProcessor();
		// processor.postProcessBeanDefinitionRegistry(context);
		// processor.enhanceConfigurationClasses(context.getBeanFactory());
		// AnnotatedBeanDefinitionReader reader = new
		// AnnotatedBeanDefinitionReader(context);
		ass(context);

		// ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(
		// context);
		context.refresh();
		// context.registerShutdownHook();

		final TickTickBean mybean = application.mybean();
		mybean.afterPropertiesSet();
		mybean.start();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				mybean.stop();
			}
		});
		application.stuff();
	}

	/**
	 * @param context
	 */
	private static void ass(GenericApplicationContext context) {
		// RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
		// context.registerBeanDefinition("test", beanDefinition);

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
				context.removeBeanDefinition(beanName);
			}
		}
	}

	@Configuration
	static class NoConfig {

	}

}
