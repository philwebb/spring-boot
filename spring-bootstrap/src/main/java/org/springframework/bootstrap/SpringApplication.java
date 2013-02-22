
package org.springframework.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.bootstrap.web.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

//FIXME DC becomes a Spring bean, can use @Configuration + combos @Import, @Enable, @ComponentScan etc

/**
 * A stand-along Spring command-line application.
 *
 * <pre>
 *
 * public class MyApplication extends SpringApplication {
 *
 * 	public static void main(String[] args) {
 * 		new MyApplication().run(args);
 * 	}
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @see #configure(Configuration, String...)
 * @see #run(String...)
 */
public class SpringApplication {

	private static String[] WEB_ENVIRONMENT_CLASSES = {
		"javax.servlet.Servlet",
		"org.springframework.web.context.ConfigurableWebApplicationContext"
	};

	private static final boolean WEB_ENVIRONMENT;
	static {
		boolean webEnvironment = true;
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			webEnvironment &= ClassUtils.isPresent(className, null);
		}
		WEB_ENVIRONMENT = webEnvironment;
	}

	private static ThreadLocal<SpringApplication> instance = new ThreadLocal<SpringApplication>();

	// FIXME DC can override ?
	public void run(String... args) {
		ConfigurationDetails configuration = new ConfigurationDetails(args);
		try {
			instance.set(this);
			configure(configuration);
			run(configuration);
		}
		catch (Exception ex) {
			// Optional call to System.exit() with status determined through e.g. a
			// protected
			// method
			ex.printStackTrace();
			System.exit(1);
		}
		finally {
			instance.set(null);
		}
	}

	/**
	 * Called to configure the application before being run. Subcalsses can override this
	 * method to customize the configuration.
	 *
	 * @param configuration the configuration
	 * @see Configuration
	 */
	protected void configure(Configuration configuration) {
	}

	private void run(ConfigurationDetails configuration) throws Exception {
		if (configuration.isBannerEnabled()) {
			Banner.write(System.out);
		}
		ApplicationContext applicationContext = createApplicationContext(configuration.getContextClass());
		registerShutdownHook(applicationContext);
		registerSelf(applicationContext);
		applyApplicationContextInitializers(applicationContext, configuration.getInitializers());
		addCommandLineProperySource(applicationContext, configuration);
		refresh(applicationContext);
		doRun(configuration, applicationContext);
	}

	protected ConfigurableApplicationContext createApplicationContext(
			Class<? extends ConfigurableApplicationContext> contextClass) {
		if (contextClass == null) {
			contextClass = WEB_ENVIRONMENT ?
					AnnotationConfigEmbeddedWebApplicationContext.class :
					AnnotationConfigApplicationContext.class;
		}
		return BeanUtils.instantiate(contextClass);
	}

	private void registerShutdownHook(ApplicationContext applicationContext) {
		if (WEB_ENVIRONMENT
				&& applicationContext instanceof ConfigurableWebApplicationContext) {
			((AbstractApplicationContext) applicationContext).registerShutdownHook();
		}
	}

	protected void applyApplicationContextInitializers(
			ApplicationContext applicationContext,
			Set<Class<? extends ApplicationContextInitializer<?>>> initializers) {
		// FIXME create, sort then call them
		// Collections.sort(this.contextInitializers, new
		// AnnotationAwareOrderComparator());
		// for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer
		// :
		// this.contextInitializers) {
		// initializer.initialize(wac);
		// }
	}

	private void addCommandLineProperySource(ApplicationContext applicationContext,
			ConfigurationDetails configuration) {
		CommandLinePropertySource<?> propertySource = createCommandLinePropertySource(configuration);
		Environment environment = applicationContext.getEnvironment();
		if (environment instanceof ConfigurableEnvironment) {
			((ConfigurableEnvironment) environment).getPropertySources().addFirst(
					propertySource);
		}
	}

	protected CommandLinePropertySource<?> createCommandLinePropertySource(
			Configuration configuration) {
		List<String> args = configuration.getArguments();
		return new SimpleCommandLinePropertySource(args.toArray(new String[args.size()]));
	}

	private void registerSelf(ApplicationContext applicationContext) {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
		configurableApplicationContext.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {
			@Override
			public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
					throws BeansException {
				BeanDefinition beanDefinition = new RootBeanDefinition(SpringApplication.this.getClass());
				beanDefinition.setFactoryMethodName("getInstance");
				registry.registerBeanDefinition("springApplication", beanDefinition);
			}

			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
					throws BeansException {
			}
		});
	}

	private void refresh(ApplicationContext applicationContext) {
		if (applicationContext instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) applicationContext).refresh();
		}
	}

	protected void doRun(ConfigurationDetails configuration,
			ApplicationContext applicationContext) {
	}

	public static SpringApplication getInstance() {
		return instance.get();
	}


	/**
	 * Callback interfaces that allows configuration of the {@link SpringApplication}.
	 * This interface is not Indented to be implemented by subclasses as it may be
	 * extended in future releases.
	 */
	protected static interface Configuration {

		/**
		 * Disable the "SpringBootstrap" banner usually logged as the application starts.
		 */
		void disableBanner();

		/**
		 * Returns a mutable list of the application arguments. This list is initially
		 * populated from the command line but may be manipulated here.
		 *
		 * @return a mutable list of arguments
		 * @see #setArguments(List)
		 */
		List<String> getArguments();

		/**
		 * Replace any existing arguments with the values from specified list.
		 *
		 * @param arguments the new arguments
		 * @see #getArguments()
		 */
		void setArguments(List<String> arguments);

		/**
		 * Set the type of Spring {@link ApplicationContext} that will be created and
		 * used.
		 *
		 * @param contextClass the application context type
		 */
		void setContextClass(Class<? extends ConfigurableApplicationContext> contextClass);

		/**
		 * Add an {@link ApplicationContextInitializer} that will called before the
		 * {@link ApplicationContext} is refreshed.
		 *
		 * @param initializer the type of initializer
		 */
		void addInitializer(Class<? extends ApplicationContextInitializer<?>> initializer); //FIXME should be ...
	}

	/**
	 * Details of a specific {@link Configuration} that may be customized.
	 */
	protected final static class ConfigurationDetails implements Configuration {

		private boolean bannerEnabled = true;

		private ArrayList<String> arguments;

		private Class<? extends ConfigurableApplicationContext> contextClass;

		private Set<Class<? extends ApplicationContextInitializer<?>>> initializers = new LinkedHashSet<Class<? extends ApplicationContextInitializer<?>>>();

		public void disableBanner() {
			this.bannerEnabled = false;
		}

		public boolean isBannerEnabled() {
			return bannerEnabled;
		}

		public ConfigurationDetails(String[] args) {
			this.arguments = new ArrayList<String>(Arrays.asList(args));
		}

		public List<String> getArguments() {
			return arguments;
		}

		public void setArguments(List<String> arguments) {
			this.arguments = new ArrayList<String>(arguments);
		}

		public Class<? extends ConfigurableApplicationContext> getContextClass() {
			return contextClass;
		}

		public void setContextClass(Class<? extends ConfigurableApplicationContext> contextClass) {
			this.contextClass = contextClass;
		}

		public Set<Class<? extends ApplicationContextInitializer<?>>> getInitializers() {
			return initializers;
		}

		public void addInitializer(
				Class<? extends ApplicationContextInitializer<?>> initializer) {
			this.initializers.add(initializer);
		}
	}


	// FIXME
	// CommandLinePropertySource
	// An API for command line options (probably duplicating commons-cli here, so maybe
	// depend on that, or maybe just allow override for options gathering so users can
	// plugin external library if needed?)
	//
	// Accept arguments on command line and optionally append from stdin.
	//
	// Clear semantics and documentation about how to shutdown the application and ensure
	// the application context is closed (depends on whether there are any non-daemon
	// threads), and allow shutdown when main() ends or on CTRL-C according to taste.
	//
	// Optional bootstrap properties in a file or Resource, or some other way to
	// initialize the Environment without writing code


}
