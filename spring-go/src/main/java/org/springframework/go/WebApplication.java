
package org.springframework.go;

import org.springframework.go.context.auto.AutoAnnotationConfigWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

public abstract class WebApplication extends AbstractDispatcherServletInitializer {

	@Override
	protected WebApplicationContext createRootApplicationContext() {
		return null;
	}

	@Override
	protected WebApplicationContext createServletApplicationContext() {
		return new AutoAnnotationConfigWebApplicationContext();
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}
/*
 *
 * Use ClassPathBeanDefinitionScanner to find beans with a special registry that allows
 * the bean to go to the appropriate location.
 *
 * We may need
 * @WebComponent
 * @WebConfiguration (if @Configuration meta annotation is supported)
 *
 * logic may also be based on type
 * ie @Controller and @ControllerAdvice should always go into wac
 *
 * may also need a more general
 * @Context("web")
 * 
 * 
 */
}
