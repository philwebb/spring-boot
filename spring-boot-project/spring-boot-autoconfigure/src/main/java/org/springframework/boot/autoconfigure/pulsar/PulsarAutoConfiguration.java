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
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Producer.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarReaderFactory;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.ReaderBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.listener.PulsarContainerProperties;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Pulsar.
 *
 * @author Chris Bono
 * @author Soby Chacko
 * @author Alexander Preuß
 * @author Phillip Webb
 * @since 3.2.0
 */
@AutoConfiguration
@Import(PulsarConfiguration.class)
public class PulsarAutoConfiguration {

	private PulsarProperties properties;

	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	DefaultPulsarProducerFactory<?> pulsarProducerFactory(Environment environment, PulsarClient pulsarClient,
			TopicResolver topicResolver) {
		String topicName = this.properties.getProducer().getTopicName();
		ProducerBuilderCustomizer<?> customizer = PulsarPropertyMapper.producerBuilderCustomizer(this.properties);
		if (!environment.getProperty("spring.pulsar.producer.cache.enabled", Boolean.class, false)) {
			return new DefaultPulsarProducerFactory<>(pulsarClient, topicName, customizer, topicResolver);
		}
		Cache cacheProperties = this.properties.getProducer().getCache();
		return new CachingPulsarProducerFactory<>(pulsarClient, topicName, customizer, topicResolver,
				cacheProperties.getExpireAfterAccess(), cacheProperties.getMaximumSize(),
				cacheProperties.getInitialCapacity());
	}

	@Bean
	@ConditionalOnMissingBean(PulsarConsumerFactory.class)
	DefaultPulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient) {
		ConsumerBuilderCustomizer<?> customizer = PulsarPropertyMapper.consumerBuilderCustomizer(this.properties);
		return new DefaultPulsarConsumerFactory<>(pulsarClient, customizer);
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarListenerContainerFactory")
	ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
			PulsarConsumerFactory<?> pulsarConsumerFactory, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		PulsarContainerProperties containerProperties = new PulsarContainerProperties();
		containerProperties.setSchemaResolver(schemaResolver);
		containerProperties.setTopicResolver(topicResolver);
		PulsarPropertyMapper.containerPropertiesCustomizer(this.properties).accept(containerProperties);
		return new ConcurrentPulsarListenerContainerFactory<>(pulsarConsumerFactory, containerProperties);
	}

	@Bean
	@ConditionalOnMissingBean(PulsarReaderFactory.class)
	DefaultPulsarReaderFactory<?> pulsarReaderFactory(PulsarClient pulsarClient) {
		ReaderBuilderCustomizer<?> readerBuilderCustomizer = PulsarPropertyMapper
			.readerBuilderCustomizer(this.properties);
		return new DefaultPulsarReaderFactory<>(pulsarClient, readerBuilderCustomizer);
	}

	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(PulsarReaderFactory<?> pulsarReaderFactory,
			SchemaResolver schemaResolver) {
		PulsarReaderContainerProperties readerContainerProperties = new PulsarReaderContainerProperties();
		readerContainerProperties.setSchemaResolver(schemaResolver);
		PulsarPropertyMapper.readerContainerPropertiesCustomizer(this.properties).accept(readerContainerProperties);
		return new DefaultPulsarReaderContainerFactory<>(pulsarReaderFactory, readerContainerProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> producerInterceptors, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		return new PulsarTemplate<>(pulsarProducerFactory, producerInterceptors.orderedStream().toList(),
				schemaResolver, topicResolver, this.properties.getTemplate().isObservationsEnabled());
	}

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	@ConditionalOnMissingBean(name = { PulsarAnnotationSupportBeanNames.PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME,
			PulsarAnnotationSupportBeanNames.PULSAR_READER_ANNOTATION_PROCESSOR_BEAN_NAME })
	static class EnablePulsarConfiguration {

	}

}
