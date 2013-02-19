
package org.springframework.bootstrap.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional} that only matches when the specified bean classes and/or names are
 * not already contained in the {@link BeanFactory}.
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnBeanCondition.class)
public @interface ConditionalOnBean {

	/**
	 * The class type of bean that should be checked.
	 * @return the class types of beans to check
	 */
	Class<?>[] value() default {};

	/**
	 * The names of beans to check.
	 * @return the name of beans to check
	 */
	String[] name() default {};

	//FIXME should we make this exactly one bean

}
