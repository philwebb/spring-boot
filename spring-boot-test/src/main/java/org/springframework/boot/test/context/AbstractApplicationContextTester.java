/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manage the lifecycle of an {@link ApplicationContext}. Such helper is best used as a
 * field of a test class, describing the shared configuration required for the test:
 *
 * <pre class="code">
 * public class FooAutoConfigurationTests {
 *
 *     private final ContextLoader contextLoader = ContextLoader.standard()
 *             .autoConfig(FooAutoConfiguration.class).env("spring.foo=bar");
 *
 * }</pre>
 *
 * <p>
 * The initialization above makes sure to register {@code FooAutoConfiguration} for all
 * tests and set the {@code spring.foo} property to {@code bar} unless specified
 * otherwise.
 *
 * <p>
 * Based on the configuration above, a specific test can simulate what would happen if the
 * user customizes a property and/or provides its own configuration:
 *
 * <pre class="code">
 * public class FooAutoConfigurationTests {
 *
 *     &#064;Test
 *     public someTest() {
 *         this.contextLoader.config(UserConfig.class).env("spring.foo=biz")
 *                 .load(context -&gt; {
 *            			// assertions using the context
 *         });
 *     }
 *
 * }</pre>
 *
 * <p>
 * The test above includes an extra {@code UserConfig} class that is guaranteed to be
 * processed <strong>before</strong> any auto-configuration. Also, {@code spring.foo} has
 * been overwritten to {@code biz}. The {@link #run(ContextConsumer) load} method takes a
 * consumer that can use the context to assert its state. Upon completion, the context is
 * automatically closed.
 *
 * <p>
 * Web environment can easily be simulated using the {@link #servletWeb()} and
 * {@link #reactiveWeb()} factory methods.
 *
 * <p>
 * If a failure scenario has to be tested, {@link #loadAndFail(Consumer)} can be used
 * instead: it expects the startup of the context to fail and call the {@link Consumer}
 * with the exception for further assertions.
 *
 * @param <SELF> The "self" type for this tester
 * @param <C> The context type
 * @param <A> The application context assertion provider
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AbstractApplicationContextTester<SELF extends AbstractApplicationContextTester<SELF, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> {

	private final Supplier<C> contextFactory;

	private final List<String> propertyValues = new ArrayList<>();

	private final List<String> systemProperties = new ArrayList<>();

	private ClassLoader classLoader;

	private ApplicationContext parent;

	private final List<Configurations> configurations = new ArrayList<>();

	/**
	 * Create a new {@link AbstractApplicationContextTester} instance.
	 * @param contextFactory the factory used to create the actual context
	 */
	protected AbstractApplicationContextTester(Supplier<C> contextFactory) {
		Assert.notNull(contextFactory, "ContextFactory must not be null");
		this.contextFactory = contextFactory;
	}

	/**
	 * Add the specified {@link Environment} property pairs. Key-value pairs can be
	 * specified with colon (":") or equals ("=") separators. Override matching keys that
	 * might have been specified previously.
	 * @param pairs the key-value pairs for properties that need to be added to the
	 * environment
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withPropertyValues(String... pairs) {
		this.propertyValues.addAll(Arrays.asList(pairs));
		return self();
	}

	/**
	 * Add the specified {@link System} property pairs. Key-value pairs can be specified
	 * with colon (":") or equals ("=") separators. System properties are added before the
	 * context is {@link #run(ContextConsumer) run} and restored when the context is
	 * closed.
	 * @param pairs the key-value pairs for properties that need to be added to the system
	 * @return this instance
	 * @see TestPropertyValues
	 * @see #withSystemProperties(String...)
	 */
	public SELF withSystemProperties(String... pairs) {
		this.systemProperties.addAll(Arrays.asList(pairs));
		return self();
	}

	/**
	 * Customize the {@link ClassLoader} that the {@link ApplicationContext} should use.
	 * Customizing the {@link ClassLoader} is an effective manner to hide resources from
	 * the classpath.
	 * @param classLoader the classloader to use (can be null to use the default)
	 * @return this instance
	 * @see HidePackagesClassLoader
	 */
	public SELF withClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return self();
	}

	/**
	 * Configure the {@link ConfigurableApplicationContext#setParent(ApplicationContext)
	 * parent} of the {@link ApplicationContext}.
	 * @param parent the parent
	 * @return this instance
	 */
	public SELF withParent(ApplicationContext parent) {
		this.parent = parent;
		return self();
	}

	/**
	 * Register the specified user configuration classes with the
	 * {@link ApplicationContext}.
	 * @param configurationClasses the user configuration classes to add
	 * @return this instance
	 */
	public SELF withUserConfiguration(Class<?>... configurationClasses) {
		return withConfiguration(UserConfigurations.of(configurationClasses));
	}

	/**
	 * Register the specified configuration classes with the {@link ApplicationContext}.
	 * @param configurations the configurations to add
	 * @return this instance
	 */
	public SELF withConfiguration(Configurations configurations) {
		Assert.notNull(configurations, "Configurations must not be null");
		this.configurations.add(configurations);
		return self();
	}

	@SuppressWarnings("unchecked")
	protected final SELF self() {
		return (SELF) this;
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader. The context is consumed by the specified {@code consumer} and closed
	 * upon completion.
	 * @param consumer the consumer of the created {@link ApplicationContext}
	 */
	public void run(ContextConsumer<A> consumer) {
		doLoad(consumer::accept);
	}

	protected void doLoad(ContextHandler<A> contextHandler) {
		try (ApplicationContextLifecycleHandler handler = new ApplicationContextLifecycleHandler()) {
			try {
				A ctx = handler.load();
				contextHandler.handle(ctx);
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"An unexpected error occurred: " + ex.getMessage(), ex);
			}
		}
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader that this expected to fail. If the context does not fail, an
	 * {@link AssertionError} is thrown. Otherwise the exception is consumed by the
	 * specified {@link Consumer} with no expectation on the type of the exception.
	 * @param consumer the consumer of the failure
	 */
	public void loadAndFail(Consumer<Throwable> consumer) {
		loadAndFail(Throwable.class, consumer);
	}

	/**
	 * Create and refresh a new {@link ApplicationContext} based on the current state of
	 * this loader that this expected to fail. If the context does not fail, an
	 * {@link AssertionError} is thrown. If the exception does not match the specified
	 * {@code exceptionType}, an {@link AssertionError} is thrown as well. If the
	 * exception type matches, it is consumed by the specified {@link Consumer}.
	 * @param exceptionType the expected type of the failure
	 * @param consumer the consumer of the failure
	 * @param <E> the expected type of the failure
	 */
	public <E extends Throwable> void loadAndFail(Class<E> exceptionType,
			Consumer<E> consumer) {
		try (ApplicationContextLifecycleHandler handler = new ApplicationContextLifecycleHandler()) {
			handler.load();
			throw new AssertionError("ApplicationContext should have failed");
		}
		catch (Throwable ex) {
			assertThat(ex).as("Wrong application context failure exception")
					.isInstanceOf(exceptionType);
			consumer.accept(exceptionType.cast(ex));
		}
	}

	private A configureApplicationContext() {
		throw new IllegalStateException();
		// C context = ContextTester.this.contextFactory.get();
		// if (this.parent != null) {
		// context.setParent(this.parent);
		// }
		// if (this.classLoader != null) {
		// Assert.isInstanceOf(DefaultResourceLoader.class, context);
		// ((DefaultResourceLoader) context).setClassLoader(this.classLoader);
		// }
		// if (!ObjectUtils.isEmpty(this.environmentProperties)) {
		// TestPropertyValues
		// .of(this.environmentProperties
		// .toArray(new String[this.environmentProperties.size()]))
		// .applyTo(context);
		// }
		// Class<?>[] configurationClasses =
		// Configurations.getClasses(this.configurations);
		// if (!ObjectUtils.isEmpty(configurationClasses)) {
		// ((AnnotationConfigRegistry) context).register(configurationClasses);
		// }
		// return context;
	}

	/**
	 * An internal callback interface that handles a concrete {@link ApplicationContext}
	 * type.
	 * @param <T> the type of the application context
	 */
	protected interface ContextHandler<T> {

		void handle(T context) throws Throwable;

	}

	/**
	 * Handles the lifecycle of the {@link ApplicationContext}.
	 */
	private class ApplicationContextLifecycleHandler implements Closeable {

		private final Map<String, String> customSystemProperties;

		private final Map<String, String> previousSystemProperties = new HashMap<>();

		private ConfigurableApplicationContext context;

		ApplicationContextLifecycleHandler() {
			this.customSystemProperties = new HashMap<>();
			// AbstractApplicationContextTester.this.systemProperties);
		}

		public A load() {
			throw new IllegalStateException();
			// setCustomSystemProperties();
			// C context = configureApplicationContext();
			// context.refresh();
			// this.context = context;
			// return context;
		}

		@Override
		public void close() {
			try {
				if (this.context != null) {
					this.context.close();
				}
			}
			finally {
				unsetCustomSystemProperties();
			}
		}

		private void setCustomSystemProperties() {
			this.customSystemProperties.forEach((key, value) -> {
				String previous = System.setProperty(key, value);
				this.previousSystemProperties.put(key, previous);
			});
		}

		private void unsetCustomSystemProperties() {
			this.previousSystemProperties.forEach((key, value) -> {
				if (value != null) {
					System.setProperty(key, value);
				}
				else {
					System.clearProperty(key);
				}
			});
		}

	}

}
