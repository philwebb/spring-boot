
package org.springframework.bootstrap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;

public class SpringApplication {

	// FIXME ArgumentAware

	private static String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",
		"org.springframework.web.context.ConfigurableWebApplicationContext" };

	private static final boolean WEB_ENVIRONMENT;
	static {
		boolean webEnvironment = true;
		for (String className : WEB_ENVIRONMENT_CLASSES) {
			webEnvironment &= ClassUtils.isPresent(className, null);
		}
		WEB_ENVIRONMENT = webEnvironment;
	}

	public void run(Class<?> mainClass, String[] args) {
		Banner.write(System.out); // FIXME make this optional
		ConfigurableApplicationContext applicationContext = createApplicationContext();
		registerShutdownHook(applicationContext);
		//FIXME applyApplicationContextInitializers
		addCommandLineProperySource(applicationContext, args);
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext;
		// FIXME how do we deal with XML

		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(mainClass);
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, "application");
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);

		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(mainClass);
		if(!metadata.isAnnotated(ComponentScan.class.getName())) {
			String[] basePackages = new String[] { ClassUtils.getPackageName(mainClass) };
			scan(mainClass, registry, basePackages);
			abd.setAttribute("componentScanBasePackages", basePackages);
		}

		refresh(applicationContext);
		List<CommandLineRunner> runners = new ArrayList<CommandLineRunner>(applicationContext.getBeansOfType(CommandLineRunner.class).values());
		AnnotationAwareOrderComparator.sort(runners);
		for (CommandLineRunner runner : runners) {
			runner.run(args);
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

	private void addCommandLineProperySource(
			ConfigurableApplicationContext applicationContext, String[] args) {
			CommandLinePropertySource<?> propertySource = new SimpleCommandLinePropertySource(args);
			Environment environment = applicationContext.getEnvironment();
			if (environment instanceof ConfigurableEnvironment) {
				((ConfigurableEnvironment) environment).getPropertySources().addFirst(
						propertySource);
			}
	}

	private void refresh(ApplicationContext applicationContext) {
		if (applicationContext instanceof AbstractApplicationContext) {
			((AbstractApplicationContext) applicationContext).refresh();
		}
	}

	private ConfigurableApplicationContext createApplicationContext() {
		Class<? extends ConfigurableApplicationContext> contextClass = WEB_ENVIRONMENT ? AnnotationConfigEmbeddedWebApplicationContext.class
				: AnnotationConfigApplicationContext.class;
		return BeanUtils.instantiate(contextClass);
	}

	private void registerShutdownHook(ApplicationContext applicationContext) {
		if (WEB_ENVIRONMENT
				&& applicationContext instanceof ConfigurableWebApplicationContext) {
			((AbstractApplicationContext) applicationContext).registerShutdownHook();
		}
	}

	public static void main(Class<?> mainClass, String[] args) {
		new SpringApplication().run(mainClass, args);
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
