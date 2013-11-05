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

import java.util.EventObject;

import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Event triggered when a {@link SpringBootCondition}.
 * 
 * @author Phillip Webb
 * @see SpringBootCondition
 */
public class ConditionEvaluationEvent extends EventObject {

	private final ConditionOutcome outcome;

	private final String message;

	/**
	 * Create a new {@link ConditionEvaluationEvent} instance.
	 * @param metadata the source meta-data
	 * @param outcome the condition outcome
	 */
	public ConditionEvaluationEvent(AnnotatedTypeMetadata metadata,
			ConditionOutcome outcome) {
		this(getClassOrMethodName(metadata), outcome);
	}

	private ConditionEvaluationEvent(String classOrMethodName, ConditionOutcome outcome) {
		super(classOrMethodName);
		this.outcome = outcome;
		this.message = buildMessage(classOrMethodName, outcome);
	}

	private String buildMessage(String classOrMethodName, ConditionOutcome outcome) {
		StringBuilder message = new StringBuilder();
		message.append("Condition ");
		message.append(ClassUtils.getShortName(getClass()));
		message.append(" on ");
		message.append(classOrMethodName);
		message.append(outcome.isMatch() ? " matched" : " did not match");
		if (StringUtils.hasLength(outcome.getMessage())) {
			message.append(" due to ");
			message.append(outcome.getMessage());
		}
		return message.toString();
	}

	public String getSourceClassOrMethodName() {
		return (String) super.getSource();
	}

	public ConditionOutcome getOutcome() {
		return this.outcome;
	}

	public String getMessage() {
		return this.message;
	}

	private static String getClassOrMethodName(AnnotatedTypeMetadata metadata) {
		if (metadata instanceof ClassMetadata) {
			ClassMetadata classMetadata = (ClassMetadata) metadata;
			return classMetadata.getClassName();
		}
		MethodMetadata methodMetadata = (MethodMetadata) metadata;
		return methodMetadata.getDeclaringClassName() + "#"
				+ methodMetadata.getMethodName();
	}

}
