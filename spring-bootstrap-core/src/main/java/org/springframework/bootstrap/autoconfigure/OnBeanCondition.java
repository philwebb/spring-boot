
package org.springframework.bootstrap.autoconfigure;

import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} that checks that specific beans are present.
 *
 * @author Phillip Webb
 * @see ConditionalOnBean
 */
class OnBeanCondition extends AbstractOnBeanCondition {

	@Override
	protected Class<?> annotationClass() {
		return ConditionalOnBean.class;
	}
}
