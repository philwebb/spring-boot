
package org.springframework.bootstrap.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableAutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {

	boolean componentScan() default true;

	// FIXME
	Class<?>[] excluding() default {};

	// FIXME should imply @ComponentScan unless @ComponentScan

}
