package org.springframework.boot.http.client.autoconfigure.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * {@link Conditional @Conditional} that matches when one or more HTTP Service bean has
 * been registered.
 *
 * @author Phillip Webb
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMissingHttpServiceProxyBeanCondition.class)
public @interface ConditionalOnMissingHttpServiceProxyBean {

}