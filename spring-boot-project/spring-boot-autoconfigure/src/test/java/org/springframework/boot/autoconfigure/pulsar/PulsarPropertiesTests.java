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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.apache.pulsar.client.api.ReaderBuilder;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.pulsar.XPulsarProperties.Admin;
import org.springframework.boot.autoconfigure.pulsar.XPulsarProperties.Client;
import org.springframework.boot.autoconfigure.pulsar.XPulsarProperties.Function;
import org.springframework.boot.autoconfigure.pulsar.XPulsarProperties.SchemaInfo;
import org.springframework.boot.autoconfigure.pulsar.XPulsarProperties.TypeMapping;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link XPulsarProperties}.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @author Soby Chacko
 */
class PulsarPropertiesTests {

	private XPulsarProperties newConfigPropsFromUserProps(Map<String, String> map) {
		XPulsarProperties targetProps = new XPulsarProperties();
		MapConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.pulsar", Bindable.ofInstance(targetProps));
		return targetProps;
	}

	@Nested
	class ClientPropertiesTests {

		@Test
		void clientProperties() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.client.service-url", "my-service-url");
			props.put("spring.pulsar.client.operation-timeout", "1s");
			props.put("spring.pulsar.client.lookup-timeout", "2s");
			props.put("spring.pulsar.client.use-tls", "true");
			props.put("spring.pulsar.client.tls-hostname-verification-enable", "true");
			props.put("spring.pulsar.client.tls-trust-certs-file-path", "my-trust-certs-file-path");
			props.put("spring.pulsar.client.tls-certificate-file-path", "my-certificate-file-path");
			props.put("spring.pulsar.client.tls-key-file-path", "my-key-file-path");
			props.put("spring.pulsar.client.tls-allow-insecure-connection", "true");
			props.put("spring.pulsar.client.use-key-store-tls", "true");
			props.put("spring.pulsar.client.ssl-provider", "my-ssl-provider");
			props.put("spring.pulsar.client.tls-trust-store-type", "my-trust-store-type");
			props.put("spring.pulsar.client.tls-trust-store-path", "my-trust-store-path");
			props.put("spring.pulsar.client.tls-trust-store-password", "my-trust-store-password");
			props.put("spring.pulsar.client.tls-ciphers[0]", "my-tls-cipher");
			props.put("spring.pulsar.client.tls-protocols[0]", "my-tls-protocol");
			props.put("spring.pulsar.client.connection-timeout", "12s");
			props.put("spring.pulsar.client.request-timeout", "13s");
			Client clientProps = newConfigPropsFromUserProps(props).getClient();

			assertThat(clientProps.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(clientProps.getOperationTimeout()).isEqualTo(Duration.ofMillis(1000));
			assertThat(clientProps.getLookupTimeout()).isEqualTo(Duration.ofMillis(2000));
			assertThat(clientProps.getConnectionTimeout()).isEqualTo(Duration.ofMillis(12000));
			assertThat(clientProps.getRequestTimeout()).isEqualTo(Duration.ofMillis(13_000));
		}

		@Test
		void authenticationUsingAuthenticationMap() {
			String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
			String authToken = "1234";
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.client.auth-plugin-class-name", authPluginClassName);
			props.put("spring.pulsar.client.authentication.token", authToken);
			XPulsarProperties configProps = newConfigPropsFromUserProps(props);
			Client clientProps = configProps.getClient();
			assertThat(clientProps.getAuthPluginClassName()).isEqualTo(authPluginClassName);
			assertThat(clientProps.getAuthentication()).containsEntry("token", authToken);
		}

	}

	@Nested
	class AdminPropertiesTests {

		private final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

		private final String authToken = "1234";

		@Test
		void adminProperties() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.administration.service-url", "my-service-url");
			props.put("spring.pulsar.administration.connection-timeout", "12s");
			props.put("spring.pulsar.administration.read-timeout", "13s");
			props.put("spring.pulsar.administration.request-timeout", "14s");
			props.put("spring.pulsar.administration.auto-cert-refresh-time", "15s");
			props.put("spring.pulsar.administration.tls-hostname-verification-enable", "true");
			props.put("spring.pulsar.administration.tls-trust-certs-file-path", "my-trust-certs-file-path");
			props.put("spring.pulsar.administration.tls-certificate-file-path", "my-certificate-file-path");
			props.put("spring.pulsar.administration.tls-key-file-path", "my-key-file-path");
			props.put("spring.pulsar.administration.tls-allow-insecure-connection", "true");
			props.put("spring.pulsar.administration.use-key-store-tls", "true");
			props.put("spring.pulsar.administration.ssl-provider", "my-ssl-provider");
			props.put("spring.pulsar.administration.tls-trust-store-type", "my-trust-store-type");
			props.put("spring.pulsar.administration.tls-trust-store-path", "my-trust-store-path");
			props.put("spring.pulsar.administration.tls-trust-store-password", "my-trust-store-password");
			props.put("spring.pulsar.administration.tls-ciphers[0]", "my-tls-cipher");
			props.put("spring.pulsar.administration.tls-protocols[0]", "my-tls-protocol");
			Admin adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(adminProps.getConnectionTimeout()).isEqualTo(Duration.ofMillis(12_000));
			assertThat(adminProps.getReadTimeout()).isEqualTo(Duration.ofMillis(13_000));
			assertThat(adminProps.getRequestTimeout()).isEqualTo(Duration.ofMillis(14_000));
		}

		@Test
		void authenticationUsingAuthenticationMap() throws UnsupportedAuthenticationException {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.administration.auth-plugin-class-name", this.authPluginClassName);
			props.put("spring.pulsar.administration.authentication.token", this.authToken);
			Admin adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getAuthPluginClassName()).isEqualTo(this.authPluginClassName);
			assertThat(adminProps.getAuthentication()).containsEntry("token", this.authToken);
		}

	}

	@Nested
	class DefaultsTypeMappingsPropertiesTests {

		@Test
		void emptyByDefault() {
			assertThat(new XPulsarProperties().getDefaults().getTypeMappings()).isEmpty();
		}

		@Test
		void withTopicsOnly() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			props.put("spring.pulsar.defaults.type-mappings[1].message-type", String.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[1].topic-name", "string-topic");
			XPulsarProperties configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings()).containsExactly(
					new TypeMapping(Foo.class, "foo-topic", null), new TypeMapping(String.class, "string-topic", null));
		}

		@Test
		void withSchemaOnly() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			XPulsarProperties configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, null, new SchemaInfo(SchemaType.JSON, null)));
		}

		@Test
		void withTopicAndSchema() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			XPulsarProperties configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, "foo-topic", new SchemaInfo(SchemaType.JSON, null)));
		}

		@Test
		void withKeyValueSchema() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "KEY_VALUE");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			XPulsarProperties configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, null, new SchemaInfo(SchemaType.KEY_VALUE, String.class)));
		}

		@Test
		void schemaTypeRequired() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("schemaType must not be null");
		}

		@Test
		void schemaTypeNoneNotAllowed() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "NONE");
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("schemaType NONE not supported");
		}

		@Test
		void messageKeyTypeOnlyAllowedForKeyValueSchemaType() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("messageKeyType can only be set when schemaType is KEY_VALUE");
		}

		record Foo(String value) {
		}

	}

	@Nested
	class ProducerPropertiesTests {

		private XPulsarProperties.Producer producerProps;

		@BeforeEach
		void producerTestProps() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.producer.topic-name", "my-topic");
			props.put("spring.pulsar.producer.name", "my-producer");
			props.put("spring.pulsar.producer.send-timeout", "2s");
			props.put("spring.pulsar.producer.message-routing-mode", "custompartition");
			props.put("spring.pulsar.producer.hashing-scheme", "murmur3_32hash");
			props.put("spring.pulsar.producer.batch.enabled", "false");
			props.put("spring.pulsar.producer.chunking-enabled", "true");
			props.put("spring.pulsar.producer.compression-type", "lz4");
			props.put("spring.pulsar.producer.access-mode", "exclusive");
			this.producerProps = newConfigPropsFromUserProps(props).getProducer();
		}

		@Test
		void producerProperties() {
			assertThat(this.producerProps.getTopicName()).isEqualTo("my-topic");
			assertThat(this.producerProps.getName()).isEqualTo("my-producer");
			assertThat(this.producerProps.getSendTimeout()).isEqualTo(Duration.ofMillis(2000));
			assertThat(this.producerProps.getMessageRoutingMode()).isEqualTo(MessageRoutingMode.CustomPartition);
			assertThat(this.producerProps.getHashingScheme()).isEqualTo(HashingScheme.Murmur3_32Hash);
			assertThat(this.producerProps.getBatch().getEnabled()).isFalse();
			assertThat(this.producerProps.getChunkingEnabled()).isTrue();
			assertThat(this.producerProps.getCompressionType()).isEqualTo(CompressionType.LZ4);
			assertThat(this.producerProps.getAccessMode()).isEqualTo(ProducerAccessMode.Exclusive);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void toProducerCustomizer() {
			ProducerBuilder producerBuilder = mock(ProducerBuilder.class);
			ProducerBuilderCustomizer<?> customizer = this.producerProps.toProducerBuilderCustomizer();
			customizer.customize(producerBuilder);

			then(producerBuilder).should().topic("my-topic");
			then(producerBuilder).should().producerName("my-producer");
			then(producerBuilder).should().sendTimeout(2_000, TimeUnit.MILLISECONDS);
			then(producerBuilder).should().messageRoutingMode(MessageRoutingMode.CustomPartition);
			then(producerBuilder).should().hashingScheme(HashingScheme.Murmur3_32Hash);
			then(producerBuilder).should().enableBatching(false);
			then(producerBuilder).should().enableChunking(true);
			then(producerBuilder).should().compressionType(CompressionType.LZ4);
			then(producerBuilder).should().accessMode(ProducerAccessMode.Exclusive);
		}

	}

	@Nested
	class ConsumerPropertiesTests {

		private XPulsarProperties.Consumer consumerProps;

		@BeforeEach
		void consumerTestProps() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.consumer.topics[0]", "my-topic");
			props.put("spring.pulsar.consumer.topics-pattern", "my-pattern");
			props.put("spring.pulsar.consumer.name", "my-consumer");
			props.put("spring.pulsar.consumer.priority-level", "8");
			props.put("spring.pulsar.consumer.read-compacted", "true");
			props.put("spring.pulsar.consumer.dead-letter-policy.max-redeliver-count", "4");
			props.put("spring.pulsar.consumer.dead-letter-policy.retry-letter-topic", "my-retry-topic");
			props.put("spring.pulsar.consumer.dead-letter-policy.dead-letter-topic", "my-dlt-topic");
			props.put("spring.pulsar.consumer.dead-letter-policy.initial-subscription-name", "my-initial-subscription");
			props.put("spring.pulsar.consumer.retry-enable", "true");
			props.put("spring.pulsar.consumer.subscription.initial-position", "earliest");
			props.put("spring.pulsar.consumer.subscription.mode", "nondurable");
			props.put("spring.pulsar.consumer.subscription.name", "my-subscription");
			props.put("spring.pulsar.consumer.subscription.topics-mode", "all-topics");
			props.put("spring.pulsar.consumer.subscription.type", "shared");
			this.consumerProps = newConfigPropsFromUserProps(props).getConsumer();
		}

		@Test
		void consumerProperties() {
			assertThat(this.consumerProps.getTopics()).containsExactly("my-topic");
			assertThat(this.consumerProps.getTopicsPattern().toString()).isEqualTo("my-pattern");
			assertThat(this.consumerProps.getName()).isEqualTo("my-consumer");
			assertThat(this.consumerProps.getPriorityLevel()).isEqualTo(8);
			assertThat(this.consumerProps.getReadCompacted()).isTrue();
			assertThat(this.consumerProps.getDeadLetterPolicy()).satisfies((dlp) -> {
				assertThat(dlp.getMaxRedeliverCount()).isEqualTo(4);
				assertThat(dlp.getRetryLetterTopic()).isEqualTo("my-retry-topic");
				assertThat(dlp.getDeadLetterTopic()).isEqualTo("my-dlt-topic");
				assertThat(dlp.getInitialSubscriptionName()).isEqualTo("my-initial-subscription");
			});
			assertThat(this.consumerProps.getRetryEnable()).isTrue();
			assertThat(this.consumerProps.getSubscription()).satisfies((subscription) -> {
				assertThat(subscription.getName()).isEqualTo("my-subscription");
				assertThat(subscription.getType()).isEqualTo(SubscriptionType.Shared);
				assertThat(subscription.getMode()).isEqualTo(SubscriptionMode.NonDurable);
				assertThat(subscription.getInitialPosition()).isEqualTo(SubscriptionInitialPosition.Earliest);
				assertThat(subscription.getTopicsMode()).isEqualTo(RegexSubscriptionMode.AllTopics);
			});
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void toConsumerCustomizer() {
			ConsumerBuilder consumerBuilder = mock(ConsumerBuilder.class);
			ConsumerBuilderCustomizer<?> customizer = this.consumerProps.toConsumerBuilderCustomizer();
			customizer.customize(consumerBuilder);
			then(consumerBuilder).should().topics(List.of("my-topic"));
			ArgumentCaptor<Pattern> argCaptor = ArgumentCaptor.forClass(Pattern.class);
			then(consumerBuilder).should().topicsPattern(argCaptor.capture());
			assertThat(argCaptor.getValue().pattern()).isEqualTo("my-pattern");
			then(consumerBuilder).should().consumerName("my-consumer");
			then(consumerBuilder).should().priorityLevel(8);
			then(consumerBuilder).should().readCompacted(true);
			then(consumerBuilder).should()
				.deadLetterPolicy(DeadLetterPolicy.builder()
					.maxRedeliverCount(4)
					.retryLetterTopic("my-retry-topic")
					.deadLetterTopic("my-dlt-topic")
					.initialSubscriptionName("my-initial-subscription")
					.build());
			then(consumerBuilder).should().enableRetry(true);
			then(consumerBuilder).should().subscriptionName("my-subscription");
			then(consumerBuilder).should().subscriptionType(SubscriptionType.Shared);
			then(consumerBuilder).should().subscriptionMode(SubscriptionMode.NonDurable);
			then(consumerBuilder).should().subscriptionInitialPosition(SubscriptionInitialPosition.Earliest);
			then(consumerBuilder).should().subscriptionTopicsMode(RegexSubscriptionMode.AllTopics);
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void deadLetterPolicyConfigIsNotRequired() {
			Map<String, String> props = new HashMap<>();
			this.consumerProps = newConfigPropsFromUserProps(props).getConsumer();
			assertThat(this.consumerProps.getDeadLetterPolicy()).isNull();
			ConsumerBuilder consumerBuilder = mock(ConsumerBuilder.class);
			ConsumerBuilderCustomizer<?> customizer = this.consumerProps.toConsumerBuilderCustomizer();
			customizer.customize(consumerBuilder);
			then(consumerBuilder).should(never()).deadLetterPolicy(any(DeadLetterPolicy.class));
		}

	}

	@Nested
	class FunctionPropertiesTests {

		@Test
		void functionPropertiesWithDefaults() {
			Map<String, String> props = new HashMap<>();
			Function functionProps = newConfigPropsFromUserProps(props).getFunction();
			assertThat(functionProps.getFailFast()).isTrue();
			assertThat(functionProps.getPropagateFailures()).isTrue();
			assertThat(functionProps.getPropagateStopFailures()).isFalse();
		}

		@Test
		void functionPropertiesWitValues() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.function.fail-fast", "false");
			props.put("spring.pulsar.function.propagate-failures", "false");
			props.put("spring.pulsar.function.propagate-stop-failures", "true");
			Function functionProps = newConfigPropsFromUserProps(props).getFunction();
			assertThat(functionProps.getFailFast()).isFalse();
			assertThat(functionProps.getPropagateFailures()).isFalse();
			assertThat(functionProps.getPropagateStopFailures()).isTrue();
		}

	}

	@Nested
	class ReaderPropertiesTests {

		private XPulsarProperties.Reader readerProps;

		@BeforeEach
		void readerTestProps() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.reader.topic-names", "my-topic");
			props.put("spring.pulsar.reader.name", "my-reader");
			props.put("spring.pulsar.reader.subscription-name", "my-subscription");
			props.put("spring.pulsar.reader.subscription-role-prefix", "sub-role");
			props.put("spring.pulsar.reader.read-compacted", "true");
			props.put("spring.pulsar.reader.reset-include-head", "true");
			this.readerProps = newConfigPropsFromUserProps(props).getReader();
		}

		@Test
		void readerProperties() {
			assertThat(this.readerProps.getTopicNames()).containsExactly("my-topic");
			assertThat(this.readerProps.getName()).isEqualTo("my-reader");
			assertThat(this.readerProps.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(this.readerProps.getSubscriptionRolePrefix()).isEqualTo("sub-role");
			assertThat(this.readerProps.getReadCompacted()).isTrue();
			assertThat(this.readerProps.getResetIncludeHead()).isTrue();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void toReaderCustomizer() {
			ReaderBuilder readerBuilder = mock(ReaderBuilder.class);
			ReaderBuilderCustomizer<?> customizer = this.readerProps.toReaderBuilderCustomizer();
			customizer.customize(readerBuilder);
			then(readerBuilder).should().topics(List.of("my-topic"));
			then(readerBuilder).should().readerName("my-reader");
			then(readerBuilder).should().subscriptionName("my-subscription");
			then(readerBuilder).should().subscriptionRolePrefix("sub-role");
			then(readerBuilder).should().readCompacted(true);
			then(readerBuilder).should().startMessageIdInclusive();
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Test
		void toReaderCustomizerResetDoesNotIncludeHead() {
			this.readerProps.setResetIncludeHead(false);
			ReaderBuilder readerBuilder = mock(ReaderBuilder.class);
			ReaderBuilderCustomizer<?> customizer = this.readerProps.toReaderBuilderCustomizer();
			customizer.customize(readerBuilder);
			then(readerBuilder).should(never()).startMessageIdInclusive();
		}

	}

}
