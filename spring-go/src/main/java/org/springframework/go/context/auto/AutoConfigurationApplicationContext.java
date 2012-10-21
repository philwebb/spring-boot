
package org.springframework.go.context.auto;

import java.util.Iterator;

import org.springframework.context.ApplicationContext;

/**
 * Extension that can be applied to {@link ApplicationContext}s that support
 * auto-configuration.
 *
 * @author Phillip Webb
 */
public interface AutoConfigurationApplicationContext extends ApplicationContext {

	/**
	 * Returns the auto configuration providers that should be called for this context.
	 *
	 * @return the providers
	 */
	Iterator<AutoConfigurationProvider> getAutoConfigurationProviders();

	//FIXME DC
	void register(Class<?>... annotatedClasses);
}
