
package org.springframework.bootstrap;

import org.springframework.bootstrap.application.Dunno;
import org.springframework.bootstrap.web.context.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class SpringApplication {

	public void run(String... args) {
		//AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
		AnnotationConfigEmbeddedWebApplicationContext applicationContext = new AnnotationConfigEmbeddedWebApplicationContext();
		applicationContext.register(getRunBean());
		applicationContext.scan(getClass().getPackage().getName());
		applicationContext.refresh();
		applicationContext.registerShutdownHook();
//		Object bean = applicationContext.getBean(getRunBean());
//		if (bean instanceof Runnable) {
//			((Runnable) bean).run();
//		}
	}

	protected Class<?> getRunBean() {
		return Dunno.class;
	}

	// CommandLinePropertySource

	// Collections.sort(this.contextInitializers, new AnnotationAwareOrderComparator());
	// for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer :
	// this.contextInitializers) {
	// initializer.initialize(wac);
	// }

	// An API for command line options (probably duplicating commons-cli here, so maybe
	// depend on that, or maybe just allow override for options gathering so users can
	// plugin external library if needed?)
	//
	// Accept arguments on command line and optionally append from stdin.
	//
	// Standard (e.g. first) argument for locating a Spring configuration (XML, package or
	// class name - I recommend sticking to a single config file and import from there if
	// there are more, or else the argument could accept wildcards).
	//
	// Autowiring of fields in the runner instance, so extensions can add @Autowired
	// fields and have them supplied magically.
	//
	// Clear semantics and documentation about how to shutdown the application and ensure
	// the application context is closed (depends on whether there are any non-daemon
	// threads), and allow shutdown when main() ends or on CTRL-C according to taste.
	//
	// Optional call to System.exit() with status determined through e.g. a protected
	// method
	//
	// A way to initialize the Environment by plugging in a context initializer a la
	// web.xml
	//
	// Optional bootstrap properties in a file or Resource, or some other way to
	// initialize the Environment without writing code
	//
	// Run a Runnable bean from the context then exit
	//
	// Wait forever

}
