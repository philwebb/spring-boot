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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.DeadLetterPolicy.DeadLetterPolicyBuilder;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.listener.AckMode;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for Apache Pulsar.
 * <p>
 * Users should refer to Pulsar documentation for complete descriptions of these
 * properties.
 *
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Christophe Bornet
 * @author Chris Bono
 * @author Kevin Lu
 * @since 3.2.0
 */
@ConfigurationProperties(prefix = "spring.pulsar")
public class PulsarProperties {

	private final Client client = new Client();

	private final Producer producer = new Producer();

	private final Consumer consumer = new Consumer();

	private final Listener listener = new Listener();

	private final Reader reader = new Reader();

	private final Function function = new Function();

	// For PulsarTemplate. Exists in both but Observations not yet in reactive
	private final Template template = new Template();

	private final Admin admin = new Admin();

	private final Defaults defaults = new Defaults();

	public Client getClient() {
		return this.client;
	}

	public Producer getProducer() {
		return this.producer;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public Listener getListener() {
		return this.listener;
	}

	public Function getFunction() {
		return this.function;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Admin getAdministration() {
		return this.admin;
	}

	public Reader getReader() {
		return this.reader;
	}

	public Defaults getDefaults() {
		return this.defaults;
	}

	public static class Client {

		/**
		 * Pulsar service URL in the format '(pulsar|pulsar+ssl)://host:port'.
		 */
		private String serviceUrl = "pulsar://localhost:6650";

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Client operation timeout.
		 */
		private Duration operationTimeout = Duration.ofSeconds(30);

		/**
		 * Client lookup timeout.
		 */
		private Duration lookupTimeout = Duration.ofMillis(-1);

		/**
		 * Duration to wait for a connection to a broker to be established.
		 */
		private Duration connectionTimeout = Duration.ofSeconds(10);

		/**
		 * Maximum duration for completing a request.
		 */
		private Duration requestTimeout = Duration.ofSeconds(60);

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public Duration getOperationTimeout() {
			return this.operationTimeout;
		}

		public void setOperationTimeout(Duration operationTimeout) {
			this.operationTimeout = operationTimeout;
		}

		public Duration getLookupTimeout() {
			return this.lookupTimeout;
		}

		public void setLookupTimeout(Duration lookupTimeout) {
			this.lookupTimeout = lookupTimeout;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

	}

	public static class Producer {

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
		private boolean chunkingEnabled;

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

		ProducerBuilderCustomizer<?> toProducerBuilderCustomizer() {
			// FIXME move?
			return (producerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopicName).to(producerBuilder::topic);
				map.from(this::getName).to(producerBuilder::producerName);
				map.from(this::getSendTimeout)
					.asInt(Duration::toMillis)
					.to(producerBuilder, (pb, val) -> pb.sendTimeout(val, TimeUnit.MILLISECONDS));
				map.from(this::getMessageRoutingMode).to(producerBuilder::messageRoutingMode);
				map.from(this::getHashingScheme).to(producerBuilder::hashingScheme);
				map.from(this::getBatch).as(Batching::getEnabled).to(producerBuilder::enableBatching);
				map.from(this::getChunkingEnabled).to(producerBuilder::enableChunking);
				map.from(this::getCompressionType).to(producerBuilder::compressionType);
				map.from(this::getAccessMode).to(producerBuilder::accessMode);
			};
		}

		public static class Batching {

			/**
			 * Whether to automatically batch messages.
			 */
			private Boolean enabled = true;

			public Boolean getEnabled() {
				return this.enabled;
			}

			public void setEnabled(Boolean enabled) {
				this.enabled = enabled;
			}

		}

		public static class Cache {

			/** Time period to expire unused entries in the cache. */
			private Duration expireAfterAccess = Duration.ofMinutes(1);

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

	}

	public static class Consumer {

		private final Subscription subscription = new Subscription();

		/**
		 * Comma-separated list of topics the consumer subscribes to.
		 */
		private Set<String> topics;

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
		private DeadLetterPolicyProperties deadLetterPolicy;

		/**
		 * Whether to auto retry messages.
		 */
		private boolean retryEnable = false;

		public Consumer.Subscription getSubscription() {
			return this.subscription;
		}

		public Set<String> getTopics() {
			return this.topics;
		}

		public void setTopics(Set<String> topics) {
			this.topics = topics;
		}

		public Pattern getTopicsPattern() {
			return this.topicsPattern;
		}

		public void setTopicsPattern(Pattern topicsPattern) {
			this.topicsPattern = topicsPattern;
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

		public DeadLetterPolicyProperties getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicyProperties deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public boolean getRetryEnable() {
			return this.retryEnable;
		}

		public void setRetryEnable(boolean retryEnable) {
			this.retryEnable = retryEnable;
		}

		ConsumerBuilderCustomizer<?> toConsumerBuilderCustomizer() {
			// FIXME?
			return (consumerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopics).as(ArrayList::new).to(consumerBuilder::topics);
				map.from(this::getTopicsPattern).to(consumerBuilder::topicsPattern);
				map.from(this::getName).to(consumerBuilder::consumerName);
				map.from(this::getPriorityLevel).to(consumerBuilder::priorityLevel);
				map.from(this::getReadCompacted).to(consumerBuilder::readCompacted);
				map.from(this::getDeadLetterPolicy)
					.as(this::toPulsarDeadLetterPolicy)
					.to(consumerBuilder::deadLetterPolicy);
				map.from(this::getRetryEnable).to(consumerBuilder::enableRetry);
				mapSubscriptionProperties(this.getSubscription(), map, consumerBuilder);
			};
		}

		/**
		 * Maps from a dead letter policy config props to a 'DeadLetterPolicy' expected by
		 * Pulsar.
		 * @param deadLetterPolicyConfig the config props defining the DLP to construct
		 * @return the Pulsar expected dead letter policy
		 */
		private DeadLetterPolicy toPulsarDeadLetterPolicy(DeadLetterPolicyProperties deadLetterPolicyConfig) {
			// FIXME
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			DeadLetterPolicyBuilder builder = DeadLetterPolicy.builder();
			DeadLetterPolicyProperties policy = this.getDeadLetterPolicy();
			map.from(policy::getMaxRedeliverCount).to(builder::maxRedeliverCount);
			map.from(policy::getRetryLetterTopic).to(builder::retryLetterTopic);
			map.from(policy::getDeadLetterTopic).to(builder::deadLetterTopic);
			map.from(policy::getInitialSubscriptionName).to(builder::initialSubscriptionName);
			return builder.build();
		}

		private void mapSubscriptionProperties(Consumer.Subscription subscription, PropertyMapper map,
				ConsumerBuilder<?> consumerBuilder) {
			// FIXME
			map.from(subscription::getInitialPosition).to(consumerBuilder::subscriptionInitialPosition);
			map.from(subscription::getMode).to(consumerBuilder::subscriptionMode);
			map.from(subscription::getName).to(consumerBuilder::subscriptionName);
			map.from(subscription::getTopicsMode).to(consumerBuilder::subscriptionTopicsMode);
			map.from(subscription::getType).to(consumerBuilder::subscriptionType);
		}

		public static class Subscription {

			/**
			 * Position where to initialize a newly created subscription.
			 */
			private SubscriptionInitialPosition initialPosition = SubscriptionInitialPosition.Latest;

			/**
			 * Subscription mode to be used when subscribing to the topic.
			 */
			private SubscriptionMode mode = SubscriptionMode.Durable;

			/**
			 * Subscription name for the consumer.
			 */
			private String name;

			/**
			 * Determines which type of topics (persistent, non-persistent, or all) the
			 * consumer should be subscribed to when using pattern subscriptions.
			 */
			private RegexSubscriptionMode topicsMode = RegexSubscriptionMode.PersistentOnly;

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

			public SubscriptionMode getMode() {
				return this.mode;
			}

			public void setMode(SubscriptionMode mode) {
				this.mode = mode;
			}

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public RegexSubscriptionMode getTopicsMode() {
				return this.topicsMode;
			}

			public void setTopicsMode(RegexSubscriptionMode topicsMode) {
				this.topicsMode = topicsMode;
			}

			public SubscriptionType getType() {
				return this.type;
			}

			public void setType(SubscriptionType type) {
				this.type = type;
			}

		}

	}

	public static class Listener {

		/**
		 * AckMode for acknowledgements. Allowed values are RECORD, BATCH, MANUAL.
		 */
		private AckMode ackMode;

		/**
		 * SchemaType of the consumed messages.
		 */
		private SchemaType schemaType;

		/**
		 * Max number of messages in a single batch request.
		 */
		private int maxNumMessages = -1;

		/**
		 * Max size in a single batch request.
		 */
		private DataSize maxNumBytes = DataSize.ofMegabytes(10);

		/**
		 * Duration to wait for enough message to fill a batch request before timing out.
		 */
		private Duration batchTimeout = Duration.ofMillis(100);

		/**
		 * Whether to record observations for receive operations when the Observations API
		 * is available.
		 */
		private Boolean observationsEnabled = true;

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public SchemaType getSchemaType() {
			return this.schemaType;
		}

		public void setSchemaType(SchemaType schemaType) {
			this.schemaType = schemaType;
		}

		public int getMaxNumMessages() {
			return this.maxNumMessages;
		}

		public void setMaxNumMessages(int maxNumMessages) {
			this.maxNumMessages = maxNumMessages;
		}

		public DataSize getMaxNumBytes() {
			return this.maxNumBytes;
		}

		public void setMaxNumBytes(DataSize maxNumBytes) {
			this.maxNumBytes = maxNumBytes;
		}

		public Duration getBatchTimeout() {
			return this.batchTimeout;
		}

		public void setBatchTimeout(Duration batchTimeout) {
			this.batchTimeout = batchTimeout;
		}

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(Boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Function {

		/**
		 * Whether to stop processing further function creates/updates when a failure
		 * occurs.
		 */
		private boolean failFast = true;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * startup while creating/updating functions.
		 */
		private boolean propagateFailures = true;

		/**
		 * Whether to throw an exception if any failure is encountered during server
		 * shutdown while enforcing stop policy on functions.
		 */
		private boolean propagateStopFailures = false;

		public boolean getFailFast() {
			return this.failFast;
		}

		public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		public boolean getPropagateFailures() {
			return this.propagateFailures;
		}

		public void setPropagateFailures(boolean propagateFailures) {
			this.propagateFailures = propagateFailures;
		}

		public boolean getPropagateStopFailures() {
			return this.propagateStopFailures;
		}

		public void setPropagateStopFailures(boolean propagateStopFailures) {
			this.propagateStopFailures = propagateStopFailures;
		}

	}

	public static class Template {

		/**
		 * Whether to record observations for send operations when the Observations API is
		 * available.
		 */
		private boolean observationsEnabled = true;

		public Boolean isObservationsEnabled() {
			return this.observationsEnabled;
		}

		public void setObservationsEnabled(boolean observationsEnabled) {
			this.observationsEnabled = observationsEnabled;
		}

	}

	public static class Admin {

		/**
		 * Pulsar web URL for the admin endpoint in the format '(http|https)://host:port'.
		 */
		private String serviceUrl = "http://localhost:8080";

		/**
		 * Fully qualified class name of the authentication plugin.
		 */
		private String authPluginClassName;

		/**
		 * Authentication parameter(s) as a JSON encoded string.
		 */
		private String authParams; // FIXME ??

		/**
		 * Authentication parameter(s) as a map of parameter names to parameter values.
		 */
		private Map<String, String> authentication;

		/**
		 * Duration to wait for a connection to server to be established.
		 */
		private Duration connectionTimeout = Duration.ofMinutes(1);

		/**
		 * Server response read time out for any request.
		 */
		private Duration readTimeout = Duration.ofMinutes(1);

		/**
		 * Server request time out for any request.
		 */
		private Duration requestTimeout = Duration.ofMinutes(5);

		public String getServiceUrl() {
			return this.serviceUrl;
		}

		public void setServiceUrl(String serviceUrl) {
			this.serviceUrl = serviceUrl;
		}

		public String getAuthPluginClassName() {
			return this.authPluginClassName;
		}

		public void setAuthPluginClassName(String authPluginClassName) {
			this.authPluginClassName = authPluginClassName;
		}

		public String getAuthParams() {
			return this.authParams;
		}

		public void setAuthParams(String authParams) {
			this.authParams = authParams;
		}

		public Map<String, String> getAuthentication() {
			return this.authentication;
		}

		public void setAuthentication(Map<String, String> authentication) {
			this.authentication = authentication;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Duration getReadTimeout() {
			return this.readTimeout;
		}

		public void setReadTimeout(Duration readTimeout) {
			this.readTimeout = readTimeout;
		}

		public Duration getRequestTimeout() {
			return this.requestTimeout;
		}

		public void setRequestTimeout(Duration requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

	}

	public static class Reader {

		/**
		 * Topic names.
		 */
		private List<String> topicNames;

		/**
		 * Reader name.
		 */
		private String name;

		/**
		 * Subscription name.
		 */
		private String subscriptionName;

		/**
		 * Prefix of subscription role.
		 */
		private String subscriptionRolePrefix;

		/**
		 * Whether to read messages from a compacted topic rather than a full message
		 * backlog of a topic.
		 */
		private Boolean readCompacted;

		/**
		 * Whether the first message to be returned is the one specified by messageId.
		 */
		private Boolean resetIncludeHead;

		public List<String> getTopicNames() {
			return this.topicNames;
		}

		public void setTopicNames(List<String> topicNames) {
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

		public String getSubscriptionRolePrefix() {
			return this.subscriptionRolePrefix;
		}

		public void setSubscriptionRolePrefix(String subscriptionRolePrefix) {
			this.subscriptionRolePrefix = subscriptionRolePrefix;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Boolean getResetIncludeHead() {
			return this.resetIncludeHead;
		}

		public void setResetIncludeHead(Boolean resetIncludeHead) {
			this.resetIncludeHead = resetIncludeHead;
		}

		ReaderBuilderCustomizer<?> toReaderBuilderCustomizer() {
			// FIXME
			return (readerBuilder) -> {
				PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
				map.from(this::getTopicNames).as(ArrayList::new).to(readerBuilder::topics);
				map.from(this::getName).to(readerBuilder::readerName);
				map.from(this::getSubscriptionName).to(readerBuilder::subscriptionName);
				map.from(this::getSubscriptionRolePrefix).to(readerBuilder::subscriptionRolePrefix);
				map.from(this::getReadCompacted).to(readerBuilder::readCompacted);
				map.from(this::getResetIncludeHead).whenTrue().to((b) -> readerBuilder.startMessageIdInclusive());
			};
		}

	}

	public static class Defaults {

		/**
		 * List of mappings from message type to topic name and schema info to use as a
		 * defaults when a topic name and/or schema is not explicitly specified when
		 * producing or consuming messages of the mapped type.
		 */
		private List<TypeMapping> typeMappings = new ArrayList<>();

		public List<TypeMapping> getTypeMappings() {
			return this.typeMappings;
		}

		public void setTypeMappings(List<TypeMapping> typeMappings) {
			this.typeMappings = typeMappings;
		}

		/**
		 * A mapping from message type to topic and/or schema info to use (at least one of
		 * {@code topicName} or {@code schemaInfo} must be specified.
		 *
		 * @param messageType the message type
		 * @param topicName the topic name
		 * @param schemaInfo the schema info
		 */
		public record TypeMapping(Class<?> messageType, String topicName, SchemaInfo schemaInfo) {

			public TypeMapping {
				Assert.notNull(messageType, "messageType must not be null");
				Assert.isTrue(topicName != null || schemaInfo != null,
						"At least one of topicName or schemaInfo must not be null");
			}

		}

		/**
		 * Represents a schema - holds enough information to construct an actual schema
		 * instance.
		 *
		 * @param schemaType schema type
		 * @param messageKeyType message key type (required for key value type)
		 */
		public record SchemaInfo(SchemaType schemaType, Class<?> messageKeyType) {

			public SchemaInfo {
				Assert.notNull(schemaType, "schemaType must not be null");
				Assert.isTrue(schemaType != SchemaType.NONE, "schemaType NONE not supported");
				Assert.isTrue(messageKeyType == null || schemaType == SchemaType.KEY_VALUE,
						"messageKeyType can only be set when schemaType is KEY_VALUE");
			}

		}

	}

}
