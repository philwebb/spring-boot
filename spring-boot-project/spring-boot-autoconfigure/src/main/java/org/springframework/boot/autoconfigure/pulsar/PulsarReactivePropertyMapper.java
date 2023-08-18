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

import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.pulsar.reactive.client.api.MutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageSenderSpec;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * Helper class used to map reactive {@link PulsarProperties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PulsarReactivePropertyMapper {

	public static Consumer<MutableReactiveMessageSenderSpec> messageSenderSpecCustomizer(PulsarProperties properties) {
		return (spec) -> customizeMessageSenderSpec(spec, properties.getProducer());
	}

	private static void customizeMessageSenderSpec(MutableReactiveMessageSenderSpec spec,
			PulsarProperties.Producer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopicName).to(spec::setTopicName);
		map.from(properties::getName).to(spec::setProducerName);
		map.from(properties::getSendTimeout).to(spec::setSendTimeout);
		map.from(properties::getMessageRoutingMode).to(spec::setMessageRoutingMode);
		map.from(properties::getHashingScheme).to(spec::setHashingScheme);
		map.from(properties.getBatch()::getEnabled).to(spec::setBatchingEnabled);
		map.from(properties::getChunkingEnabled).to(spec::setChunkingEnabled);
		map.from(properties::getCompressionType).to(spec::setCompressionType);
		map.from(properties::getAccessMode).to(spec::setAccessMode);
	}

	public static Consumer<MutableReactiveMessageConsumerSpec> messageConsumerSpecCustomizer(
			PulsarProperties properties) {
		return (spec) -> customizeMessageConsumerSpec(spec, properties.getConsumer());
	}

	private static void customizeMessageConsumerSpec(MutableReactiveMessageConsumerSpec spec,
			PulsarProperties.Consumer properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopics).as(ArrayList::new).to(spec::setTopicNames);
		map.from(properties::getTopicsPattern).to(spec::setTopicsPattern);
		map.from(properties::getDeadLetterPolicy).as(DeadLetterPolicyMapper::map).to(spec::setDeadLetterPolicy);
		// FIXME missing, does it mapper?
		// map.from(properties::getRetryLetterTopicEnable).to(spec::setRetryLetterTopicEnable);
		map.from(properties::getName).to(spec::setConsumerName);
		map.from(properties::getPriorityLevel).to(spec::setPriorityLevel);
		map.from(properties::getReadCompacted).to(spec::setReadCompacted);
		// FIXME missing, does it mapper?
		// map.from(properties::getTopicsPatternSubscriptionMode).to(spec::setTopicsPatternSubscriptionMode);
		customizeMessageConsumerSpecSubscription(spec, properties.getSubscription());
	}

	private static void customizeMessageConsumerSpecSubscription(MutableReactiveMessageConsumerSpec spec,
			PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getName).to(spec::setSubscriptionName);
		map.from(properties::getType).to(spec::setSubscriptionType);
		map.from(properties::getMode).to(spec::setSubscriptionMode);
		map.from(properties::getInitialPosition).to(spec::setSubscriptionInitialPosition);
	}

	public static Consumer<ReactivePulsarContainerProperties<?>> containerPropertiesCustomizer(
			PulsarProperties properties) {
		return (containerProperties) -> customizeContainerProperties(containerProperties, properties);
	}

	private static void customizeContainerProperties(ReactivePulsarContainerProperties<?> containerProperties,
			PulsarProperties properties) {
		customizePulsarContainerConsumerSubscriptionProperties(containerProperties,
				properties.getConsumer().getSubscription());
		customizePulsarContainerListenerProperties(containerProperties, properties.getListener());
	}

	private static void customizePulsarContainerConsumerSubscriptionProperties(
			ReactivePulsarContainerProperties<?> containerProperties,
			PulsarProperties.Consumer.Subscription properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getType).to(containerProperties::setSubscriptionType);
	}

	private static void customizePulsarContainerListenerProperties(
			ReactivePulsarContainerProperties<?> containerProperties, PulsarProperties.Listener properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		// FIXME
		// map.from(listenerProperties::getSchemaType).to(containerProperties::setSchemaType);
		// map.from(listenerProperties::getHandlingTimeout).to(containerProperties::setHandlingTimeout);
		// map.from(listenerProperties::getUseKeyOrderedProcessing).to(containerProperties::setUseKeyOrderedProcessing);
	}

	public static Consumer<MutableReactiveMessageReaderSpec> messageReaderSpecCustomizer(PulsarProperties properties) {
		return (spec) -> customizeMessageReaderSpec(spec, properties.getReader());
	}

	private static void customizeMessageReaderSpec(MutableReactiveMessageReaderSpec spec,
			PulsarProperties.Reader properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopicNames).as(ArrayList::new).to(spec::setTopicNames);
		map.from(properties::getName).to(spec::setReaderName);
		map.from(properties::getSubscriptionName).to(spec::setSubscriptionName);
		// FIXME
		// map.from(properties::getGeneratedSubscriptionNamePrefix).to(spec::setGeneratedSubscriptionNamePrefix);
		map.from(properties::getReadCompacted).to(spec::setReadCompacted);
		// FIXME
		// map.from(properties::getKeyHashRanges).as(List::of).to(spec::setKeyHashRanges);
	}

}
