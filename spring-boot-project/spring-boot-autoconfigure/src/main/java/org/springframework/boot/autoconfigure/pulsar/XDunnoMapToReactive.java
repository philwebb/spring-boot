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

import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderSpec;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

/**
 * @author pwebb
 */
public class XDunnoMapToReactive {

	private void pulsarReactiveAnnotationDrivenConfigurationReactivePulsarListenerContainerFactory() {
		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		containerProperties.setSubscriptionType(this.properties.getConsumer().getSubscription().getType());
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		PulsarProperties.Listener listenerProperties = this.properties.getListener();
		map.from(listenerProperties::getSchemaType).to(containerProperties::setSchemaType);
		map.from(listenerProperties::getHandlingTimeout).to(containerProperties::setHandlingTimeout);
		map.from(listenerProperties::getUseKeyOrderedProcessing).to(containerProperties::setUseKeyOrderedProcessing);

	}

	/**
	 * @param properties
	 * @return
	 */
	public static ReactiveMessageSenderSpec buildReactiveMessageSenderSpec(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param properties
	 * @return
	 */
	public static ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param properties
	 * @return
	 */
	public static ReactiveMessageReaderSpec buildReactiveMessageReaderSpec(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
