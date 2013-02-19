
package org.springframework.bootstrap.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks that specific beans are missing.
 *
 * @author Phillip Webb
 * @see ConditionalOnMissingBean
 */
class OnMissingBeanCondition extends AbstractOnBeanCondition {

	@Override
	protected Class<?> annotationClass() {
		return ConditionalOnMissingBean.class;
	}

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return !super.matches(context, metadata);
	}
}
