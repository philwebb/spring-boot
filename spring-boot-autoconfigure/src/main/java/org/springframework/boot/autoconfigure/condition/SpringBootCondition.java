/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Base of all {@link Condition} implementations used with Spring Boot. Provides sensible
 * logging to help the user diagnose what classes are loaded.
 * 
 * @author Phillip Webb
 * @author Greg Turnquist
 */
public abstract class SpringBootCondition implements Condition {

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionOutcome outcome = getMatchOutcome(context, metadata);
		ConditionEvaluationEvent event = new ConditionEvaluationEvent(context, metadata,
				outcome);
		logEvent(event);
		// AutoConfigurationReport.registerDecision(context, event);
		return outcome.isMatch();
	}

	private void logEvent(ConditionEvaluationEvent event) {
		if (event.getOutcome().isMatch() && this.logger.isDebugEnabled()) {
			this.logger.debug(event.getMessage());
		}
		else if (!event.getOutcome().isMatch() && this.logger.isTraceEnabled()) {
			this.logger.trace(event.getMessage());
		}
	}

	/**
	 * Determine the outcome of the match along with suitable log output.
	 */
	public abstract ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata);

	protected final boolean anyMatches(ConditionContext context,
			AnnotatedTypeMetadata metadata, Condition... conditions) {
		for (Condition condition : conditions) {
			if (matches(context, metadata, condition)) {
				return true;
			}
		}
		return false;
	}

	protected final boolean matches(ConditionContext context,
			AnnotatedTypeMetadata metadata, Condition condition) {
		if (condition instanceof SpringBootCondition) {
			return ((SpringBootCondition) condition).getMatchOutcome(context, metadata)
					.isMatch();
		}
		return condition.matches(context, metadata);
	}

}
