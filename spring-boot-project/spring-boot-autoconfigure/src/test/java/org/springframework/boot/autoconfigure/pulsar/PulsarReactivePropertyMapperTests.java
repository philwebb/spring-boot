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
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageSenderSpec;
import org.junit.jupiter.api.Test;

import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PulsarReactivePropertyMapper}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PulsarReactivePropertyMapperTests {

	@Test
	void messageSenderSpecCustomizer() {
		PulsarProperties properties = new PulsarProperties();
		properties.getProducer().setName("name");
		properties.getProducer().setTopicName("topicname");
		properties.getProducer().setSendTimeout(Duration.ofSeconds(1));
		properties.getProducer().setMessageRoutingMode(MessageRoutingMode.RoundRobinPartition);
		properties.getProducer().setHashingScheme(HashingScheme.JavaStringHash);
		properties.getProducer().setBatchingEnabled(false);
		properties.getProducer().setChunkingEnabled(true);
		properties.getProducer().setCompressionType(CompressionType.SNAPPY);
		properties.getProducer().setAccessMode(ProducerAccessMode.Exclusive);
		MutableReactiveMessageSenderSpec producerBuilder = new MutableReactiveMessageSenderSpec();
		PulsarReactivePropertyMapper.reactiveMessageSenderBuilderCustomizer(properties).accept(producerBuilder);
		assertThat(producerBuilder.getProducerName()).isEqualTo("name");
		assertThat(producerBuilder.getTopicName()).isEqualTo("topicname");
		assertThat(producerBuilder.getSendTimeout()).isEqualTo(Duration.ofSeconds(1));
		assertThat(producerBuilder.getMessageRoutingMode()).isEqualTo(MessageRoutingMode.RoundRobinPartition);
		assertThat(producerBuilder.getHashingScheme()).isEqualTo(HashingScheme.JavaStringHash);
		assertThat(producerBuilder.getBatchingEnabled()).isFalse();
		assertThat(producerBuilder.getChunkingEnabled()).isTrue();
		assertThat(producerBuilder.getCompressionType()).isEqualTo(CompressionType.SNAPPY);
		assertThat(producerBuilder.getAccessMode()).isEqualTo(ProducerAccessMode.Exclusive);
	}

	@Test
	void customizeMessageConsumerSpec() {
		PulsarProperties properties = new PulsarProperties();
		List<String> topics = List.of("mytopic");
		Pattern topisPattern = Pattern.compile("my-pattern");
		properties.getConsumer().setName("name");
		properties.getConsumer().setTopics(topics);
		properties.getConsumer().setTopicsPattern(topisPattern);
		properties.getConsumer().setPriorityLevel(123);
		properties.getConsumer().setReadCompacted(true);
		properties.getConsumer().getDeadLetterPolicy().setDeadLetterTopic("my-dlt");
		MutableReactiveMessageConsumerSpec consumerBuilder = new MutableReactiveMessageConsumerSpec();
		PulsarReactivePropertyMapper.messageConsumerSpecCustomizer(properties).accept(consumerBuilder);
		assertThat(consumerBuilder.getConsumerName()).isEqualTo("name");
		assertThat(consumerBuilder.getTopicNames()).isEqualTo(topics);
		assertThat(consumerBuilder.getTopicsPattern()).isEqualTo(topisPattern);
		assertThat(consumerBuilder.getPriorityLevel()).isEqualTo(123);
		assertThat(consumerBuilder.getReadCompacted()).isEqualTo(true);
		assertThat(consumerBuilder.getDeadLetterPolicy()).isEqualTo(new DeadLetterPolicy(0, null, "my-dlt", null));
	}

	@Test
	void containerPropertiesCustomizer() {
		PulsarProperties properties = new PulsarProperties();
		properties.getConsumer().getSubscription().setType(SubscriptionType.Shared);
		properties.getListener().setSchemaType(SchemaType.AVRO);
		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		PulsarReactivePropertyMapper.containerPropertiesCustomizer(properties).accept(containerProperties);
		assertThat(containerProperties.getSubscriptionType()).isEqualTo(SubscriptionType.Shared);
		assertThat(containerProperties.getSchemaType()).isEqualTo(SchemaType.AVRO);
	}

	@Test
	void messageReaderSpecCustomizer() {
		List<String> topics = List.of("my-topic");
		PulsarProperties properties = new PulsarProperties();
		properties.getReader().setName("name");
		properties.getReader().setTopics(topics);
		properties.getReader().setSubscriptionName("subname");
		properties.getReader().setSubscriptionRolePrefix("srp");
		MutableReactiveMessageReaderSpec readerBuilder = new MutableReactiveMessageReaderSpec();
		PulsarReactivePropertyMapper.messageReaderSpecCustomizer(properties).accept(readerBuilder);
		assertThat(readerBuilder.getReaderName()).isEqualTo("name");
		assertThat(readerBuilder.getTopicNames()).isEqualTo(topics);
		assertThat(readerBuilder.getSubscriptionName()).isEqualTo("subname");
		assertThat(readerBuilder.getGeneratedSubscriptionNamePrefix()).isEqualTo("srp");
	}

}
