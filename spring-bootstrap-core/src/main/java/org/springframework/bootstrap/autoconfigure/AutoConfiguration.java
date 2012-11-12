package org.springframework.bootstrap.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Annotation used on beans that attempt to auto configure the application context.
 * Implementations should also be registered in {@code META-INF/spring.factories}.
 *
 * @author Phillip Webb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
@Conditional(NotDisabledCondition.class)
public @interface AutoConfiguration {
}
