package org.springframework.bootstrap.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when the specified classes are on the classpath.
 * @author Phillip Webb
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {

	/**
	 * The classes that must be present.  Since this annotation parsed by loading
	 * class bytecode it is safe to specify classes here that may ultimately not
	 * be on the classpath.
	 * @return the classes that must be present
	 */
	public Class<?>[] value() default {};

	/**
	 * The classes names that must be present.  When possible {@link #value()} should be
	 * used in preference to this property.
	 * @return the class names that must be present.
	 */
	public String[] name() default {};
}
