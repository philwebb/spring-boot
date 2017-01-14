/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.context.annotation.ConditionContext;

/**
 * A special {@link SpringBootCondition SpringBootConditions} that can also be evaluated
 * when auto-configuration classes are first imported. Allows early evaluation saving ASM
 * bytecode parsing entirely.
 *
 * @author Phillip Webb
 * @since 1.5.0
 */
public abstract class SpringBootAutoConfigurationCondition extends SpringBootCondition {

	public final boolean matches(ConditionContext context, String autoConfigurationClass,
			ConditionEvaluationReport report) {
		try {
			ConditionOutcome outcome = getMatchOutcome(context, autoConfigurationClass);
			logOutcome(autoConfigurationClass, outcome);
			report.recordConditionEvaluation(autoConfigurationClass, this, outcome);
			return outcome.isMatch();
		}
		catch (Throwable ex) {
			// Will be re-evaluted using AnnotatedTypeMetadata
			return false;
		}

	}

	/**
	 * Determine the outcome of the match along with suitable log output.
	 * @param context the condition context
	 * @param autoConfigurationClass the auto configuration class
	 * @return the condition outcome
	 */
	abstract ConditionOutcome getMatchOutcome(ConditionContext context,
			String autoConfigurationClass);

}
