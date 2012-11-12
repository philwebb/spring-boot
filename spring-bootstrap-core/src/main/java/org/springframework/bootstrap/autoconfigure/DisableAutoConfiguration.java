package org.springframework.bootstrap.autoconfigure;


/**
 * Disable a specific {@link AutoConfiguration} class so that it will never apply.  This
 * annotation can be used to disable one or more {@link AutoConfiguration} class so that
 * are never considered.  Can be used to provide fine-graned control over
 * auto-configuration.  This method can be placed on any {@code @Configuration} bean.
 * @author Phillip Webb
 */
public @interface DisableAutoConfiguration {

	/**
	 * Returns the {@code @AutoConfiguration} annotated classes that should be disabled.
	 * @return the classes to disable
	 */
	Class<?>[] value();

}
