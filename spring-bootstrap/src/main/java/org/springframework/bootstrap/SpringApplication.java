
package org.springframework.bootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.StandardClassMetadata;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;

public class SpringApplication {

	// FIXME ApplicationContextInitiaizer support
	// FIXME Profile command line support

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


	private Object[] sources;

	private boolean showBanner = true;

	private boolean addCommandLineProperties = true;

	private ResourceLoader resourceLoader;

	private BeanNameGenerator beanNameGenerator;

	private ConfigurableEnvironment environment;

	private ApplicationContext applicationContext;

	private Class<? extends ApplicationContext> applicationContextClass;


	public SpringApplication(Object... sources) {
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
	}

	public SpringApplication(ResourceLoader resourceLoader, Object... sources) {
		Assert.notEmpty(sources, "Sources must not be empty");
		this.sources = sources;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}


	public ApplicationContext run(String... args) {
		printBanner();
		ApplicationContext context = createApplicationContext();
		postProcessApplicationContext(context);
		addPropertySources(context, args);
		load(context);
		refresh(context);
		runCommandLineRunners(context, args);
		return context;
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

	protected void postProcessApplicationContext(ApplicationContext context) {
		if(WEB_ENVIRONMENT) {
			if(context instanceof ConfigurableWebApplicationContext) {
				ConfigurableWebApplicationContext configurableContext = (ConfigurableWebApplicationContext) context;
				if(this.beanNameGenerator != null) {
					configurableContext.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, this.beanNameGenerator);
				}
			}
		}

		if(context instanceof AbstractApplicationContext) {
			((AbstractApplicationContext)context).setEnvironment(this.environment);
		}

		if(context instanceof GenericApplicationContext) {
			((GenericApplicationContext)context).setResourceLoader(this.resourceLoader);
		}
	}

	protected void addPropertySources(ApplicationContext context, String[] args) {
		if(this.addCommandLineProperties) {
			CommandLinePropertySource<?> propertySource = new SimpleCommandLinePropertySource(args);
			Environment environment = context.getEnvironment();
			if (environment instanceof ConfigurableEnvironment) {
				((ConfigurableEnvironment) environment).getPropertySources().addFirst(propertySource);
			}
		}
	}

	protected void load(ApplicationContext context) {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, context);
		BeanDefinitionLoader loader = new BeanDefinitionLoader((BeanDefinitionRegistry)context, this.sources);
		loader.setBeanNameGenerator(this.beanNameGenerator);
		loader.setResourceLoader(this.resourceLoader);
		loader.setEnvironment(this.environment);
		loader.load();
	}

	private void runCommandLineRunners(ApplicationContext context, String... args) {
		List<CommandLineRunner> runners = new ArrayList<CommandLineRunner>(context.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (CommandLineRunner runner : runners) {
			runner.run(args);
		}
	}

	protected void refresh(ApplicationContext applicationContext) {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		((AbstractApplicationContext) applicationContext).refresh();
	}

	public void setShowBanner(boolean showBanner) {
		this.showBanner = showBanner;
	}

	public void setAddCommandLineProperties(boolean addCommandLineProperties) {
		this.addCommandLineProperties = addCommandLineProperties;
	}

	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
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
	}

	public static void run(Object[] sources, String[] args) {
		new SpringApplication(sources).run(args);
	}

	public static void run(Object source, String[] args) {
		new SpringApplication(source).run(args);
	}

	public static void runComponents(Class<?>[] classes, String[] args) {
		List<Class<?>> componentClasses = new ArrayList<Class<?>>();
		for (Class<?> candidate : classes) {
			StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(candidate);
			if(metadata.isAnnotated(Component.class.getName())) {
				componentClasses.add(candidate);
			}
		}
		new SpringApplication(componentClasses.toArray()).run(args);
	}


}
