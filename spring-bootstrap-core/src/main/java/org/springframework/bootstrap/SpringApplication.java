
package org.springframework.bootstrap;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.bootstrap.autoconfigure.AutoConfigurationApplicationContextInitializer;
import org.springframework.bootstrap.autoconfigure.AutoConfigurationClassPostProcessor;
import org.springframework.bootstrap.web.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;

//FIXME DC default to component scanning, use @Import or @ImportResource or configure to change.

/**
 * A stand-along Spring command-line application.
 *
 * <pre>
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

	private static final boolean WEB_ENVIRONMENT =
			ClassUtils.isPresent("javax.servlet.Servlet", null) &&
			ClassUtils.isPresent("org.springframework.web.context.ConfigurableWebApplicationContext", null);

	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	// FIXME DC can override
	public void run(String... args) {
		ConfigurationDetails configuration = new ConfigurationDetails(args);
		try {
			initilizeConfiguration(configuration);
			configure(configuration);
			run(configuration);
		} catch (Exception ex) {
			// Optional call to System.exit() with status determined through e.g. a
			// protected
			// method
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Initialize the configuration based on any class level annotations.
	 *
	 * @param configuration the configuration
	 */
	private void initilizeConfiguration(ConfigurationDetails configuration) {
		Import importAnnotation = getClass().getAnnotation(Import.class);
		if (importAnnotation != null) {
			configuration.addImport(importAnnotation.value());
		}
		ImportResource importResourceAnnotation = getClass().getAnnotation(
				ImportResource.class);
		if (importResourceAnnotation != null) {
			configuration.addImport(importResourceAnnotation.value());
			configuration.setImportReader(importResourceAnnotation.reader());
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
		if(configuration.isBannerEnabled()) {
			System.out.println();
			Banner.write(System.out);
			System.out.println();
			System.out.println();
		}
		ApplicationContext applicationContext = createApplicationContext(configuration.getContextClass());
		registerShutdownHook(applicationContext);
		setupImports(applicationContext, configuration);
		applyApplicationContextInitializers(applicationContext,
				configuration.getInitializers());
		if(configuration.isAutoConfigure()) {
			new AutoConfigurationApplicationContextInitializer().initialize((ConfigurableApplicationContext) applicationContext);
		}
		addCommandLineProperySource(applicationContext, configuration);
		refresh(applicationContext);
		if (configuration.isAutowireSelf()) {
			applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
		}
		doRun(configuration, applicationContext);
	}

	protected void doRun(ConfigurationDetails configuration,
			ApplicationContext applicationContext) {
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

	protected ApplicationContext createApplicationContext(
			Class<? extends ApplicationContext> contextClass) {
		if (contextClass == null) {
			contextClass = WEB_ENVIRONMENT ?
					AnnotationConfigEmbeddedWebApplicationContext.class :
					AnnotationConfigApplicationContext.class;
		}
		return BeanUtils.instantiate(contextClass);
	}

	protected void setupImports(ApplicationContext applicationContext,
			ConfigurationDetails configuration) {
		if (!configuration.getImports().isEmpty()
				|| StringUtils.hasLength(configuration.getContextConfigLocation())) {
			// FIXME configure user customized stuff
		}
		else {
			// Fallback to scanning form the application class
			try {
				Method scanMethod = ReflectionUtils.findMethod(
						applicationContext.getClass(), "scan", String[].class);
				Assert.state(scanMethod != null,
						"Unable to find scan method on application context "
								+ applicationContext.getClass());
				ReflectionUtils.invokeMethod(scanMethod, applicationContext,
						new Object[] { getScanBasePackage() });
			} catch (Exception ex) {
				throw new IllegalStateException("Unable to scan for bean", ex);
			}
		}
	}

	protected String[] getScanBasePackage() {
		return new String[] { getClass().getPackage().getName() };
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

	private void registerShutdownHook(ApplicationContext applicationContext) {
		if (WEB_ENVIRONMENT && applicationContext instanceof ConfigurableWebApplicationContext) {
			((AbstractApplicationContext) applicationContext).registerShutdownHook();
		}
	}

	private void refresh(ApplicationContext applicationContext) {
		if (applicationContext instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) applicationContext).refresh();
		}
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
		void setContextClass(Class<? extends ApplicationContext> contextClass);

		/**
		 * Add an {@link ApplicationContextInitializer} that will called before the
		 * {@link ApplicationContext} is refreshed.
		 *
		 * @param initializer the type of initializer
		 */
		void addInitializer(Class<? extends ApplicationContextInitializer<?>> initializer);

		/**
		 * Import the specified resource when the application context is created. Adding
		 * imports will disable the default strategy of searching for beans.
		 *
		 * @param importResource the resource to import
		 * @see #addImport(Class)
		 * @see #setImportReader(Class)
		 */
		void addImport(String... importResource);

		/**
		 * Import the specified class when the application context is created. Adding
		 * imports will disable the default strategy of searching for beans.
		 *
		 * @param importClass the class to import
		 * @see #addImport(Class)
		 * @see #setImportReader(Class)
		 */
		void addImport(Class<?>... importClass);

		/**
		 * Set the reader that will be used to load imported resources. If not specified
		 * the {@link XmlBeanDefinitionReader} will be used.
		 *
		 * @param importReader the import reader
		 */
		void setImportReader(Class<? extends BeanDefinitionReader> importReader);

		/**
		 * Set the context configuration location. Setting a context config location will
		 * disable the default strategy of searching for beans.
		 *
		 * @param contextConfigLocation
		 */
		void setContextConfigLocation(String contextConfigLocation);

		/**
		 * Disable if the {@link SpringApplication} itself will be autowired with bean
		 * from the application context. Whilst the {@link SpringApplication} is not
		 * itself a managed bean it can be injected with managed objects.
		 */
		void disableAutowireSelf();

		/**
		 * Disable all {@link AutoConfigurationClassPostProcessor auto-configuration} of
		 * the application context.
		 */
		void disableAutoConfigure();
	}

	/**
	 * Details of a specific {@link Configuration} that may be customized.
	 */
	protected final static class ConfigurationDetails implements Configuration {

		private boolean bannerEnabled = true;

		private ArrayList<String> arguments;

		private Class<? extends ApplicationContext> contextClass;

		private Set<Class<? extends ApplicationContextInitializer<?>>> initializers = new LinkedHashSet<Class<? extends ApplicationContextInitializer<?>>>();

		private Set<Object> imports = new LinkedHashSet<Object>();

		private Class<? extends BeanDefinitionReader> importReader;

		private String contextConfigLocation;

		private boolean autowireSelf = true;

		private boolean autoConfigure = true;


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

		public Class<? extends ApplicationContext> getContextClass() {
			return contextClass;
		}

		public void setContextClass(Class<? extends ApplicationContext> contextClass) {
			this.contextClass = contextClass;
		}

		public Set<Class<? extends ApplicationContextInitializer<?>>> getInitializers() {
			return initializers;
		}

		public void addInitializer(
				Class<? extends ApplicationContextInitializer<?>> initializer) {
			this.initializers.add(initializer);
		}

		public Set<Object> getImports() {
			return imports;
		}

		public void addImport(String... importResource) {
			this.imports.addAll(Arrays.asList(importResource));
		}

		public void addImport(Class<?>... importClass) {
			this.imports.add(Arrays.asList(importClass));
		}

		public Class<? extends BeanDefinitionReader> getImportReader() {
			return importReader;
		}

		public void setImportReader(Class<? extends BeanDefinitionReader> importReader) {
			this.importReader = importReader;
		}

		public String getContextConfigLocation() {
			return contextConfigLocation;
		}

		public void setContextConfigLocation(String contextConfigLocation) {
			this.contextConfigLocation = contextConfigLocation;
		}

		public boolean isAutowireSelf() {
			return autowireSelf;
		}

		public void disableAutowireSelf() {
			this.autowireSelf = false;
		}

		public boolean isAutoConfigure() {
			return autoConfigure;
		}

		public void disableAutoConfigure() {
			this.autoConfigure = false;
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
