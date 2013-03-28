
package org.springframework.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AbstractTypeHierarchyTraversingFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;

public class SpringApplication {

	// FIXME ArgumentAware interface
	// FIXME ApplicationContextInitiaizer support
	// FIXME Profile command line support
	// FIXME Support for XML bean definitions ?
	// FIXME support for a specific application context
	// FIXME Disable banner?

	private static final boolean WEB_ENVIRONMENT;
	static {
		boolean webEnvironment = true;
		for (String className : Arrays.asList(
				"javax.servlet.Servlet",
				"org.springframework.web.context.ConfigurableWebApplicationContext")) {
			webEnvironment &= ClassUtils.isPresent(className, null);
		}
		WEB_ENVIRONMENT = webEnvironment;
	}

	private static final String DEFAULT_CONTEXT_CLASS =
			"org.springframework.context.annotation.AnnotationConfigApplicationContext";

	private static final String DEFAULT_WEB_CONTEXT_CLASS =
			"org.springframework.web.context.embedded.AnnotationConfigEmbeddedWebApplicationContext";


	private ApplicationContext applicationContext;

	private Class<? extends ApplicationContext> applicationContextClass;

	private boolean showBanner = true;

	private boolean addCommandLineProperties = true;


	public void run(Class<?> applicationClass, String... args) {
		printBanner();
		ApplicationContext applicationContext = createApplicationContext();
		addCommandLineProperySource(applicationContext, args);

		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
		// FIXME how do we deal with XML

		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(applicationClass);
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, "application");
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(applicationClass);
		if(!metadata.isAnnotated(ComponentScan.class.getName())) {
			String[] basePackages = new String[] { ClassUtils.getPackageName(applicationClass) };
			scan(applicationClass, registry, basePackages);
			abd.setAttribute("componentScanBasePackages", basePackages);
		}

		refresh(applicationContext);
		List<CommandLineRunner> runners = new ArrayList<CommandLineRunner>(applicationContext.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (CommandLineRunner runner : runners) {
			runner.run(args);
		}

	}

	protected void printBanner() {
		if(this.showBanner) {
			Banner.write(System.out);
		}
	}

	protected ApplicationContext createApplicationContext() {
		if(this.applicationContext != null) {
			return this.applicationContext;
		}

		Class<?> contextClass = this.applicationContextClass;
		if(contextClass == null) {
			try {
				contextClass = Class.forName(
						(WEB_ENVIRONMENT ? DEFAULT_WEB_CONTEXT_CLASS : DEFAULT_CONTEXT_CLASS));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Unable create a default ApplicationContext, " +
						"please specify an ApplicationContextClass", ex);
			}
		}

		return (ApplicationContext) BeanUtils.instantiate(contextClass);
	}

	protected void addCommandLineProperySource(ApplicationContext applicationContext, String[] args) {
		if(this.addCommandLineProperties) {
			CommandLinePropertySource<?> propertySource = new SimpleCommandLinePropertySource(args);
			Environment environment = applicationContext.getEnvironment();
			if (environment instanceof ConfigurableEnvironment) {
				((ConfigurableEnvironment) environment).getPropertySources().addFirst(propertySource);
			}
		}
	}



	protected void scan(final Class<?> mainClass, BeanDefinitionRegistry registry, String[] basePackages) {
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
		scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false){
			@Override
			protected boolean matchClassName(String className) {
				return mainClass.getName().equals(className);
			}
		});
		scanner.scan(basePackages);
	}



	private void refresh(ApplicationContext applicationContext) {
		if (applicationContext instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) applicationContext).refresh();
		}
	}


	/**
	 * Sets a Spring {@link ApplicationContext} that will be used for the application. If
	 * not specified an {@link AnnotationConfigEmbeddedWebApplicationContext} will be
	 * created for web based applications or an {@link AnnotationConfigApplicationContext}
	 * for non web based applications.
	 * @param applicationContext the spring application context.
	 * @see #setApplicationContextClass(Class)
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Sets the type of Spring {@link ApplicationContext} that will be created. If not
	 * specified defaults to {@link AnnotationConfigEmbeddedWebApplicationContext} for web
	 * based applications or {@link AnnotationConfigApplicationContext} for non web based
	 * applications.
	 * @param applicationContextClass the context class to set
	 * @see #setApplicationContext(ApplicationContext)
	 */
	public void setApplicationContextClass(
			Class<? extends ApplicationContext> applicationContextClass) {
		this.applicationContextClass = applicationContextClass;
	}


	public static void main(String[] args) {
		// FIXME inspect 1st item. class, package xml
		try {
			Class<?> applicationClass = Class.forName(args[0]);
			String[] remainingArgs = new String[args.length-1];
			System.arraycopy(args, 1, remainingArgs, 0, args.length - 1);
			main(applicationClass, remainingArgs);
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(Class<?> applicationClass, String[] args) {
		new SpringApplication().run(applicationClass, args);
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
