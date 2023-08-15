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
import org.apache.pulsar.client.api.DeadLetterPolicy.DeadLetterPolicyBuilder;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.Range;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configuration properties for Spring for the Apache Pulsar reactive client.
 * <p>
 * Users should refer to Pulsar reactive client documentation for complete descriptions of
 * these properties.
 *
 * @author Christophe Bornet
 * @author Chris Bono
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.pulsar.reactive")
public class XXPulsarReactiveProperties {

	private final Sender sender = new Sender();

	private final Consumer consumer = new Consumer();

	private final Listener listener = new Listener();

	private final Reader reader = new Reader();

	public Sender getSender() {
		return this.sender;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Listener getListener() {
		return this.listener;
	}

	public ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
		return this.sender.buildReactiveMessageSenderSpec();
	}

	public ReactiveMessageReaderSpec buildReactiveMessageReaderSpec() {
		return this.reader.buildReactiveMessageReaderSpec();
	}

	public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {
		return this.consumer.buildReactiveMessageConsumerSpec();
	}

	public static class Sender {

		/**
		 * Topic the producer will publish to.
		 */
		private String topicName;

		/**
		 * Name for the producer. If not assigned, a unique name is generated.
		 */
		private String name;

		/**
		 * Time before a message has to be acknowledged by the broker.
		 */
		private Duration sendTimeout = Duration.ofSeconds(30);

		/**
		 * Message routing mode for a partitioned producer.
		 */
		private MessageRoutingMode messageRoutingMode = MessageRoutingMode.RoundRobinPartition;

		/**
		 * Message hashing scheme to choose the partition to which the message is
		 * published.
		 */
		private HashingScheme hashingScheme = HashingScheme.JavaStringHash;

		/**
		 * Whether to split large-size messages into multiple chunks.
		 */
		private boolean chunkingEnabled = false;

		/**
		 * Message compression type.
		 */
		private CompressionType compressionType;

		/**
		 * Type of access to the topic the producer requires.
		 */
		private ProducerAccessMode accessMode = ProducerAccessMode.Shared;

		private final Batching batch = new Batching();

		private final Cache cache = new Cache();

		public String getTopicName() {
			return this.topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Duration getSendTimeout() {
			return this.sendTimeout;
		}

		public void setSendTimeout(Duration sendTimeout) {
			this.sendTimeout = sendTimeout;
		}

		public MessageRoutingMode getMessageRoutingMode() {
			return this.messageRoutingMode;
		}

		public void setMessageRoutingMode(MessageRoutingMode messageRoutingMode) {
			this.messageRoutingMode = messageRoutingMode;
		}

		public HashingScheme getHashingScheme() {
			return this.hashingScheme;
		}

		public void setHashingScheme(HashingScheme hashingScheme) {
			this.hashingScheme = hashingScheme;
		}

		public boolean getChunkingEnabled() {
			return this.chunkingEnabled;
		}

		public void setChunkingEnabled(boolean chunkingEnabled) {
			this.chunkingEnabled = chunkingEnabled;
		}

		public CompressionType getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(CompressionType compressionType) {
			this.compressionType = compressionType;
		}

		public ProducerAccessMode getAccessMode() {
			return this.accessMode;
		}

		public void setAccessMode(ProducerAccessMode accessMode) {
			this.accessMode = accessMode;
		}

		public Batching getBatch() {
			return this.batch;
		}

		public Cache getCache() {
			return this.cache;
		}

		ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			MutableReactiveMessageSenderSpec spec = new MutableReactiveMessageSenderSpec();
			map.from(this::getTopicName).to(spec::setTopicName);
			map.from(this::getName).to(spec::setProducerName);
			map.from(this::getSendTimeout).to(spec::setSendTimeout);
			map.from(this::getMessageRoutingMode).to(spec::setMessageRoutingMode);
			map.from(this::getHashingScheme).to(spec::setHashingScheme);
			map.from(this::getBatch).as(Batching::getEnabled).to(spec::setBatchingEnabled);
			map.from(this::getChunkingEnabled).to(spec::setChunkingEnabled);
			map.from(this::getCompressionType).to(spec::setCompressionType);
			map.from(this::getAccessMode).to(spec::setAccessMode);
			return new ImmutableReactiveMessageSenderSpec(spec);
		}

	}

	public static class Batching {

		/**
		 * Whether to automatically batch messages.
		 */
		private boolean enabled = true;

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class Reader {

		/**
		 * Topic names.
		 */
		private String[] topicNames;

		/**
		 * Reader name.
		 */
		private String name;

		/**
		 * Subscription name.
		 */
		private String subscriptionName;

		/**
		 * Prefix to use when auto-generating a subscription name.
		 */
		private String generatedSubscriptionNamePrefix;

		/**
		 * Whether to read messages from a compacted topic rather than a full message
		 * backlog of a topic.
		 */
		private Boolean readCompacted;

		/**
		 * Key hash ranges of the reader.
		 */
		private Range[] keyHashRanges;

		public String[] getTopicNames() {
			return this.topicNames;
		}

		public void setTopicNames(String[] topicNames) {
			this.topicNames = topicNames;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSubscriptionName() {
			return this.subscriptionName;
		}

		public void setSubscriptionName(String subscriptionName) {
			this.subscriptionName = subscriptionName;
		}

		public String getGeneratedSubscriptionNamePrefix() {
			return this.generatedSubscriptionNamePrefix;
		}

		public void setGeneratedSubscriptionNamePrefix(String generatedSubscriptionNamePrefix) {
			this.generatedSubscriptionNamePrefix = generatedSubscriptionNamePrefix;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Range[] getKeyHashRanges() {
			return this.keyHashRanges;
		}

		public void setKeyHashRanges(Range[] keyHashRanges) {
			this.keyHashRanges = keyHashRanges;
		}

		public ReactiveMessageReaderSpec buildReactiveMessageReaderSpec() {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			MutableReactiveMessageReaderSpec spec = new MutableReactiveMessageReaderSpec();

			map.from(this::getTopicNames).as(List::of).to(spec::setTopicNames);
			map.from(this::getName).to(spec::setReaderName);
			map.from(this::getSubscriptionName).to(spec::setSubscriptionName);
			map.from(this::getGeneratedSubscriptionNamePrefix).to(spec::setGeneratedSubscriptionNamePrefix);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getKeyHashRanges).as(List::of).to(spec::setKeyHashRanges);

			return new ImmutableReactiveMessageReaderSpec(spec);
		}

	}

	public static class Consumer {

		private final Subscription subscription = new Subscription();

		/**
		 * Comma-separated list of topics the consumer subscribes to.
		 */
		private String[] topics;

		/**
		 * Pattern for topics the consumer subscribes to.
		 */
		private Pattern topicsPattern;

		/**
		 * Consumer name to identify a particular consumer from the topic stats.
		 */
		private String name;

		/**
		 * Priority level for shared subscription consumers.
		 */
		private int priorityLevel = 0;

		/**
		 * Whether to read messages from the compacted topic rather than the full message
		 * backlog.
		 */
		private boolean readCompacted = false;

		/**
		 * Dead letter policy to use.
		 */
		@NestedConfigurationProperty
		private XDeadLetterPolicyProperties deadLetterPolicy;

		/**
		 * Whether the retry letter topic is enabled.
		 */
		private boolean retryLetterTopicEnable = false;

		/**
		 * Determines which topics the consumer should be subscribed to when using pattern
		 * subscriptions.
		 */
		private RegexSubscriptionMode topicsPatternSubscriptionMode = RegexSubscriptionMode.PersistentOnly;

		public String[] getTopics() {
			return this.topics;
		}

		public void setTopics(String[] topics) {
			this.topics = topics;
		}

		public Pattern getTopicsPattern() {
			return this.topicsPattern;
		}

		public void setTopicsPattern(Pattern topicsPattern) {
			this.topicsPattern = topicsPattern;
		}

		public XDeadLetterPolicyProperties getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(XDeadLetterPolicyProperties deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public boolean getRetryLetterTopicEnable() {
			return this.retryLetterTopicEnable;
		}

		public void setRetryLetterTopicEnable(boolean retryLetterTopicEnable) {
			this.retryLetterTopicEnable = retryLetterTopicEnable;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getPriorityLevel() {
			return this.priorityLevel;
		}

		public void setPriorityLevel(int priorityLevel) {
			this.priorityLevel = priorityLevel;
		}

		public boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public RegexSubscriptionMode getTopicsPatternSubscriptionMode() {
			return this.topicsPatternSubscriptionMode;
		}

		public void setTopicsPatternSubscriptionMode(RegexSubscriptionMode topicsPatternSubscriptionMode) {
			this.topicsPatternSubscriptionMode = topicsPatternSubscriptionMode;
		}

		public Subscription getSubscription() {
			return this.subscription;
		}

		ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {
			MutableReactiveMessageConsumerSpec spec = new MutableReactiveMessageConsumerSpec();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getTopics).as(List::of).to(spec::setTopicNames);
			map.from(this::getTopicsPattern).to(spec::setTopicsPattern);
			map.from(this::getDeadLetterPolicy).as(this::toPulsarDeadLetterPolicy).to(spec::setDeadLetterPolicy);
			map.from(this::getRetryLetterTopicEnable).to(spec::setRetryLetterTopicEnable);
			map.from(this::getName).to(spec::setConsumerName);
			map.from(this::getPriorityLevel).to(spec::setPriorityLevel);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getTopicsPatternSubscriptionMode).to(spec::setTopicsPatternSubscriptionMode);
			mapSubscriptionProperties(this.getSubscription(), map, spec);
			return new ImmutableReactiveMessageConsumerSpec(spec);
		}

		/**
		 * Maps from a dead letter policy config props to a 'DeadLetterPolicy' expected by
		 * Pulsar.
		 * @param dlpConfigProps the config props defining the DLP to construct
		 * @return the Pulsar expected dead letter policy
		 */
		private DeadLetterPolicy toPulsarDeadLetterPolicy(XDeadLetterPolicyProperties dlpConfigProps) {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			DeadLetterPolicyBuilder dlpBuilder = DeadLetterPolicy.builder();
			map.from(dlpConfigProps::getMaxRedeliverCount).to(dlpBuilder::maxRedeliverCount);
			map.from(dlpConfigProps::getRetryLetterTopic).to(dlpBuilder::retryLetterTopic);
			map.from(dlpConfigProps::getDeadLetterTopic).to(dlpBuilder::deadLetterTopic);
			map.from(dlpConfigProps::getInitialSubscriptionName).to(dlpBuilder::initialSubscriptionName);
			return dlpBuilder.build();
		}

		private void mapSubscriptionProperties(Subscription subscription, PropertyMapper map,
				MutableReactiveMessageConsumerSpec spec) {
			map.from(subscription::getName).to(spec::setSubscriptionName);
			map.from(subscription::getType).to(spec::setSubscriptionType);
			map.from(subscription::getMode).to(spec::setSubscriptionMode);
			map.from(subscription::getInitialPosition).to(spec::setSubscriptionInitialPosition);
		}

	}

	public static class Subscription {

		/**
		 * Position where to initialize a newly created subscription.
		 */
		private SubscriptionInitialPosition initialPosition = SubscriptionInitialPosition.Latest;

		/**
		 * Subscription name for the consumer.
		 */
		private String name;

		/**
		 * Subscription mode to be used when subscribing to the topic.
		 */
		private SubscriptionMode mode = SubscriptionMode.Durable;

		/**
		 * Subscription type to be used when subscribing to a topic.
		 */
		private SubscriptionType type = SubscriptionType.Exclusive;

		public SubscriptionInitialPosition getInitialPosition() {
			return this.initialPosition;
		}

		public void setInitialPosition(SubscriptionInitialPosition initialPosition) {
			this.initialPosition = initialPosition;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SubscriptionType getType() {
			return this.type;
		}

		public void setType(SubscriptionType type) {
			this.type = type;
		}

		public SubscriptionMode getMode() {
			return this.mode;
		}

		public void setMode(SubscriptionMode mode) {
			this.mode = mode;
		}

	}

	public enum SchedulerType {

		/**
		 * The reactor.core.scheduler.BoundedElasticScheduler.
		 */
		boundedElastic,

		/**
		 * The reactor.core.scheduler.ParallelScheduler.
		 */
		parallel,

		/**
		 * The reactor.core.scheduler.SingleScheduler.
		 */
		single,

		/**
		 * The reactor.core.scheduler.ImmediateScheduler.
		 */
		immediate

	}

	public static class Cache {

		/** Time period after last access to expire unused entries in the cache. */
		private Duration expireAfterAccess = Duration.ofMinutes(1);

		/** Time period after last write to expire unused entries in the cache. */
		private Duration expireAfterWrite = Duration.ofMinutes(10);

		/** Maximum size of cache (entries). */
		private long maximumSize = 1000L;

		/** Initial size of cache. */
		private int initialCapacity = 50;

		public Duration getExpireAfterAccess() {
			return this.expireAfterAccess;
		}

		public void setExpireAfterAccess(Duration expireAfterAccess) {
			this.expireAfterAccess = expireAfterAccess;
		}

		public Duration getExpireAfterWrite() {
			return this.expireAfterWrite;
		}

		public void setExpireAfterWrite(Duration expireAfterWrite) {
			this.expireAfterWrite = expireAfterWrite;
		}

		public long getMaximumSize() {
			return this.maximumSize;
		}

		public void setMaximumSize(long maximumSize) {
			this.maximumSize = maximumSize;
		}

		public int getInitialCapacity() {
			return this.initialCapacity;
		}

		public void setInitialCapacity(int initialCapacity) {
			this.initialCapacity = initialCapacity;
		}

	}

	public static class Listener {

		// FIXME quite different from regular

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Duration to wait before the message handling times out.
		 */
		private Duration handlingTimeout = Duration.ofMinutes(2);

		/**
		 * Whether per-key message ordering should be maintained when concurrent
		 * processing is used.
		 */
		private boolean useKeyOrderedProcessing = false;

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public Duration getHandlingTimeout() {
			return this.handlingTimeout;
		}

		public void setHandlingTimeout(Duration handlingTimeout) {
			this.handlingTimeout = handlingTimeout;
		}

		public boolean getUseKeyOrderedProcessing() {
			return this.useKeyOrderedProcessing;
		}

		public void setUseKeyOrderedProcessing(boolean useKeyOrderedProcessing) {
			this.useKeyOrderedProcessing = useKeyOrderedProcessing;
		}

	}

}
