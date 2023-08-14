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
import java.util.HashMap;
import java.util.Map;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.Range;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderSpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.pulsar.PulsarReactiveProperties.Cache;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PulsarReactiveProperties}.
 *
 * @author Christophe Bornet
 * @author Chris Bono
 */
class PulsarReactivePropertiesTests {

	private PulsarReactiveProperties newConfigPropsFromUserProps(Map<String, String> map) {
		PulsarReactiveProperties targetProps = new PulsarReactiveProperties();
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.pulsar.reactive", Bindable.ofInstance(targetProps));
		return targetProps;
	}

	@Nested
	class SenderPropertiesTests {

		@Test
		void senderPropsToSenderSpec() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.reactive.sender.topic-name", "my-topic");
			props.put("spring.pulsar.reactive.sender.name", "my-producer");
			props.put("spring.pulsar.reactive.sender.send-timeout", "2s");
			props.put("spring.pulsar.reactive.sender.message-routing-mode", "custompartition");
			props.put("spring.pulsar.reactive.sender.hashing-scheme", "murmur3_32hash");
			props.put("spring.pulsar.reactive.sender.batch.enabled", "false");
			props.put("spring.pulsar.reactive.sender.chunking-enabled", "true");
			props.put("spring.pulsar.reactive.sender.compression-type", "lz4");
			props.put("spring.pulsar.reactive.sender.access-mode", "exclusive");

			PulsarReactiveProperties configProps = newConfigPropsFromUserProps(props);
			ReactiveMessageSenderSpec senderSpec = configProps.buildReactiveMessageSenderSpec();

			assertThat(senderSpec.getTopicName()).isEqualTo("my-topic");
			assertThat(senderSpec.getProducerName()).isEqualTo("my-producer");
			assertThat(senderSpec.getSendTimeout()).isEqualTo(Duration.ofSeconds(2));
			assertThat(senderSpec.getHashingScheme()).isEqualTo(HashingScheme.Murmur3_32Hash);
			assertThat(senderSpec.getBatchingEnabled()).isEqualTo(false);
			assertThat(senderSpec.getChunkingEnabled()).isEqualTo(true);
			assertThat(senderSpec.getCompressionType()).isEqualTo(CompressionType.LZ4);
			assertThat(senderSpec.getAccessMode()).isEqualTo(ProducerAccessMode.Exclusive);
		}

		@Test
		void senderCacheProps() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.reactive.sender.cache.expire-after-access", "100s");
			props.put("spring.pulsar.reactive.sender.cache.expire-after-write", "200s");
			props.put("spring.pulsar.reactive.sender.cache.maximum-size", "5150");
			props.put("spring.pulsar.reactive.sender.cache.initial-capacity", "200");
			Cache cacheProps = newConfigPropsFromUserProps(props).getSender().getCache();
			assertThat(cacheProps.getExpireAfterAccess()).isEqualTo(Duration.ofSeconds(100));
			assertThat(cacheProps.getExpireAfterWrite()).isEqualTo(Duration.ofSeconds(200));
			assertThat(cacheProps.getMaximumSize()).isEqualTo(5150L);
			assertThat(cacheProps.getInitialCapacity()).isEqualTo(200L);
		}

	}

	@Nested
	class ConsumerPropertiesTests {

		@Test
		void consumerPropsToConsumerSpec() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.reactive.consumer.topics[0]", "my-topic");
			props.put("spring.pulsar.reactive.consumer.topics-pattern", "my-pattern");
			props.put("spring.pulsar.reactive.consumer.dead-letter-policy.max-redeliver-count", "4");
			props.put("spring.pulsar.reactive.consumer.dead-letter-policy.retry-letter-topic", "my-retry-topic");
			props.put("spring.pulsar.reactive.consumer.dead-letter-policy.dead-letter-topic", "my-dlt-topic");
			props.put("spring.pulsar.reactive.consumer.dead-letter-policy.initial-subscription-name",
					"my-initial-subscription");
			props.put("spring.pulsar.reactive.consumer.retryLetterTopicEnable", "true");
			props.put("spring.pulsar.reactive.consumer.name", "my-consumer");
			props.put("spring.pulsar.reactive.consumer.priority-level", "8");
			props.put("spring.pulsar.reactive.consumer.read-compacted", "true");
			props.put("spring.pulsar.reactive.consumer.topics-pattern-subscription-mode", "alltopics");
			props.put("spring.pulsar.reactive.consumer.subscription.initial-position", "earliest");
			props.put("spring.pulsar.reactive.consumer.subscription.name", "my-subscription");
			props.put("spring.pulsar.reactive.consumer.subscription.type", "shared");
			props.put("spring.pulsar.reactive.consumer.subscription.mode", "nondurable");

			PulsarReactiveProperties configProps = newConfigPropsFromUserProps(props);
			ReactiveMessageConsumerSpec consumerSpec = configProps.buildReactiveMessageConsumerSpec();

			assertThat(consumerSpec.getTopicNames()).containsExactly("my-topic");
			assertThat(consumerSpec.getTopicsPattern().toString()).isEqualTo("my-pattern");
			assertThat(consumerSpec.getDeadLetterPolicy().getMaxRedeliverCount()).isEqualTo(4);
			assertThat(consumerSpec.getDeadLetterPolicy().getRetryLetterTopic()).isEqualTo("my-retry-topic");
			assertThat(consumerSpec.getDeadLetterPolicy().getDeadLetterTopic()).isEqualTo("my-dlt-topic");
			assertThat(consumerSpec.getDeadLetterPolicy().getInitialSubscriptionName())
				.isEqualTo("my-initial-subscription");
			assertThat(consumerSpec.getRetryLetterTopicEnable()).isTrue();
			assertThat(consumerSpec.getConsumerName()).isEqualTo("my-consumer");
			assertThat(consumerSpec.getPriorityLevel()).isEqualTo(8);
			assertThat(consumerSpec.getReadCompacted()).isTrue();
			assertThat(consumerSpec.getTopicsPatternSubscriptionMode()).isEqualTo(RegexSubscriptionMode.AllTopics);
			assertThat(consumerSpec.getSubscriptionInitialPosition()).isEqualTo(SubscriptionInitialPosition.Earliest);
			assertThat(consumerSpec.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(consumerSpec.getSubscriptionType()).isEqualTo(SubscriptionType.Shared);
			assertThat(consumerSpec.getSubscriptionMode()).isEqualTo(SubscriptionMode.NonDurable);
		}

		@SuppressWarnings("unchecked")
		@Test
		void deadLetterPolicyConfigIsNotRequired() {
			Map<String, String> props = new HashMap<>();
			PulsarReactiveProperties configProps = newConfigPropsFromUserProps(props);
			ReactiveMessageConsumerSpec consumerSpec = configProps.buildReactiveMessageConsumerSpec();
			assertThat(consumerSpec.getDeadLetterPolicy()).isNull();
		}

	}

	@Nested
	class ReaderPropertiesTests {

		@Test
		void readerPropsToReaderSpec() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.reactive.reader.topic-names[0]", "my-topic");
			props.put("spring.pulsar.reactive.reader.name", "my-reader");
			props.put("spring.pulsar.reactive.reader.subscription-name", "my-subscription");
			props.put("spring.pulsar.reactive.reader.generated-subscription-name-prefix", "my-prefix");
			props.put("spring.pulsar.reactive.reader.read-compacted", "true");
			props.put("spring.pulsar.reactive.reader.key-hash-ranges[0].start", "2");
			props.put("spring.pulsar.reactive.reader.key-hash-ranges[0].end", "3");

			PulsarReactiveProperties configProps = newConfigPropsFromUserProps(props);
			ReactiveMessageReaderSpec readerSpec = configProps.buildReactiveMessageReaderSpec();

			assertThat(readerSpec.getTopicNames()).containsExactly("my-topic");
			assertThat(readerSpec.getReaderName()).isEqualTo("my-reader");
			assertThat(readerSpec.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(readerSpec.getGeneratedSubscriptionNamePrefix()).isEqualTo("my-prefix");
			assertThat(readerSpec.getReadCompacted()).isTrue();
			assertThat(readerSpec.getKeyHashRanges()).containsExactly(Range.of(2, 3));
		}

	}

}
