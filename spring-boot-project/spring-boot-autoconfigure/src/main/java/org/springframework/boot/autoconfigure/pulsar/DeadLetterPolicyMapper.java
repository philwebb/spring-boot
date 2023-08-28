/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.DeadLetterPolicy.DeadLetterPolicyBuilder;

import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Helper class used to map {@link PulsarProperties.Consumer.DeadLetterPolicy dead letter
 * policy properties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class DeadLetterPolicyMapper {

	static DeadLetterPolicy map(PulsarProperties.Consumer.DeadLetterPolicy policy) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		DeadLetterPolicyBuilder builder = DeadLetterPolicy.builder();
		map.from(policy::getMaxRedeliverCount).to(builder::maxRedeliverCount);
		map.from(policy::getRetryLetterTopic).to(builder::retryLetterTopic);
		map.from(policy::getDeadLetterTopic).to(builder::deadLetterTopic);
		map.from(policy::getInitialSubscriptionName).to(builder::initialSubscriptionName);
		return builder.build();
	}

}