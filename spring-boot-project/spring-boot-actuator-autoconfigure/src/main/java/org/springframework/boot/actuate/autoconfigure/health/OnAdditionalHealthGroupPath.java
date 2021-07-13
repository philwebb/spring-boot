/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Madhura Bhave
 **/
class OnAdditionalHealthGroupPath extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage.forCondition("Additional health group paths");
		String prefix = (String) metadata
				.getAnnotationAttributes(ConditionalOnAdditionalHealthGroupPath.class.getName()).get("prefix");
		BindResult<Map<String, HealthEndpointProperties.Group>> bindResult = Binder.get(context.getEnvironment()).bind(
				"management.endpoint.health.group", Bindable.mapOf(String.class, HealthEndpointProperties.Group.class));
		if (!bindResult.isBound()) {
			return ConditionOutcome.noMatch(message.because("No additional paths specified"));
		}
		Map<String, HealthEndpointProperties.Group> groups = bindResult.get();
		boolean matchingPaths = groups.values().stream()
				.anyMatch((g) -> g.getAdditionalPath() != null && g.getAdditionalPath().startsWith(prefix));
		if (matchingPaths) {
			return ConditionOutcome.match(message.because("Additional paths specified with prefix " + prefix));
		}
		return ConditionOutcome.noMatch(message.because("Additional paths not specified with prefix " + prefix));
	}

}
