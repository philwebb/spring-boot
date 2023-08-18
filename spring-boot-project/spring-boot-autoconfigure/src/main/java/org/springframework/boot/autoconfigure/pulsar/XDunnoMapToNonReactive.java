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

import java.time.Duration;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Consumer;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Producer;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Reader;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;
import org.springframework.util.unit.DataSize;

/**
 * @author pwebb
 */
public class XDunnoMapToNonReactive {

	private PulsarContainerProperties getContainerProperties(SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		PulsarContainerProperties container = new PulsarContainerProperties();
		container.setSchemaResolver(schemaResolver);
		container.setTopicResolver(topicResolver);
		container.setSubscriptionType(this.properties.getConsumer().getSubscription().getType());
		mapListenerProperties(container);
		return container;
	}

	private void mapListenerProperties(PulsarContainerProperties container) {
		XPulsarProperties.Listener properties = this.properties.getListener();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getSchemaType).to(container::setSchemaType);
		map.from(properties::getAckMode).to(container::setAckMode);
		map.from(properties::getBatchTimeout).asInt(Duration::toMillis).to(container::setBatchTimeoutMillis);
		map.from(properties::getMaxNumBytes).asInt(DataSize::toBytes).to(container::setMaxNumBytes);
		map.from(properties::getMaxNumMessages).to(container::setMaxNumMessages);
		map.from(properties::isObservationsEnabled).to(container::setObservationEnabled);
	}

	private void readerStuff() {
		PulsarReaderContainerProperties reader = new PulsarReaderContainerProperties();
		reader.setSchemaResolver(schemaResolver);
		XPulsarProperties.Reader properties = this.properties.getReader();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getTopicNames).to(reader::setTopics);

	}

	/**
	 * @param producer
	 * @return
	 */
	public static ProducerBuilderCustomizer<?> toProducerBuilderCustomizer(Producer producer) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param consumer
	 * @return
	 */
	public static ConsumerBuilderCustomizer<?> toConsumerBuilderCustomizer(Consumer consumer) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param reader
	 * @return
	 */
	public static ReaderBuilderCustomizer<?> toReaderBuilderCustomizer(Reader reader) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
