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

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.reactive.client.adapter.ProducerCacheProvider;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderCache;
import org.apache.pulsar.reactive.client.api.ReactivePulsarClient;
import org.apache.pulsar.reactive.client.producercache.CaffeineShadedProducerCacheProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.annotation.EnableReactivePulsar;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring for Apache Pulsar
 * Reactive.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @since 3.2.0
 */
@AutoConfiguration(after = PulsarAutoConfiguration.class)
@Import(PulsarConfiguration.class)
public class PulsarReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarClient reactivePulsarClient(PulsarClient pulsarClient) {
		return null;
	}

	@Bean
	@ConditionalOnMissingBean(ProducerCacheProvider.class)
	@ConditionalOnClass(CaffeineShadedProducerCacheProvider.class)
	@ConditionalOnProperty(name = "spring.pulsar.reactive.producer.cache.enabled", havingValue = "true",
			matchIfMissing = true)
	CaffeineShadedProducerCacheProvider reactivePulsarProducerCacheProvider() {
		return null; // XPulsarReactiveAutoConfiguration
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.reactive.producer.cache.enabled", havingValue = "true",
			matchIfMissing = true)
	ReactiveMessageSenderCache reactivePulsarMessageSenderCache(
			ObjectProvider<ProducerCacheProvider> producerCacheProvider) {
		return null; // XPulsarReactiveAutoConfiguration
	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarSenderFactory.class)
	DefaultReactivePulsarSenderFactory<?> reactivePulsarSenderFactory(ReactivePulsarClient pulsarReactivePulsarClient,
			ObjectProvider<ReactiveMessageSenderCache> cache, TopicResolver topicResolver) {
		return null; // XPulsarReactiveAutoConfiguration

	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarConsumerFactory.class)
	DefaultReactivePulsarConsumerFactory<?> reactivePulsarConsumerFactory(
			ReactivePulsarClient pulsarReactivePulsarClient) {
		return null; // XPulsarReactiveAutoConfiguration

	}

	@Bean
	@ConditionalOnMissingBean(name = "reactivePulsarListenerContainerFactory")
	DefaultReactivePulsarListenerContainerFactory<?> reactivePulsarListenerContainerFactory(
			ObjectProvider<ReactivePulsarConsumerFactory<Object>> consumerFactoryProvider,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return null; // XPulsarReactiveAnnotationDrivenConfiguration
	}

	@Bean
	@ConditionalOnMissingBean(ReactivePulsarReaderFactory.class)
	DefaultReactivePulsarReaderFactory<?> reactivePulsarReaderFactory(ReactivePulsarClient pulsarReactivePulsarClient) {
		return null; // XPulsarReactiveAutoConfiguration

	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarTemplate<?> pulsarReactiveTemplate(ReactivePulsarSenderFactory<?> reactivePulsarSenderFactory,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return null; // XPulsarReactiveAutoConfiguration

	}

	@Configuration(proxyBeanMethods = false)
	@EnableReactivePulsar
	@ConditionalOnMissingBean(
			name = PulsarAnnotationSupportBeanNames.REACTIVE_PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableReactivePulsarConfiguration {

	}

}
