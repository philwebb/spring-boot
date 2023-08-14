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

import java.util.List;
import java.util.Optional;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.SchemaInfo;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.TypeMapping;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Producer.Cache;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarClientFactory;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarReaderFactory;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunction;
import org.springframework.pulsar.function.PulsarFunctionAdministration;
import org.springframework.pulsar.function.PulsarSink;
import org.springframework.pulsar.function.PulsarSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Apache Pulsar.
 *
 * @author Soby Chacko
 * @author Chris Bono
 * @author Alexander Preuß
 * @since 3.2.0
 */
@AutoConfiguration
@ConditionalOnClass(PulsarTemplate.class)
@EnableConfigurationProperties(PulsarProperties.class)
@Import({ PulsarAnnotationDrivenConfiguration.class })
public class PulsarAutoConfiguration {

	private final PulsarProperties properties;

	PulsarAutoConfiguration(PulsarProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarClient pulsarClient(ObjectProvider<PulsarClientBuilderCustomizer> customizersProvider)
			throws PulsarClientException {
		PulsarClientBuilderCustomizers customizers = new PulsarClientBuilderCustomizers();
		customizers.add(new PulsarPropertiesClientBuilderCustomizer(this.properties));
		customizers.addAll(customizersProvider.orderedStream());
		return new DefaultPulsarClientFactory(customizers).createClient();
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "false")
	DefaultPulsarProducerFactory<?> pulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver) {
		String defaultTopic = this.properties.getProducer().getTopicName();
		ProducerBuilderCustomizer<?> defaultConfigCustomizer = this.properties.getProducer()
			.toProducerBuilderCustomizer();
		return new DefaultPulsarProducerFactory<>(pulsarClient, defaultTopic, defaultConfigCustomizer, topicResolver);
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	CachingPulsarProducerFactory<?> cachingPulsarProducerFactory(PulsarClient pulsarClient,
			TopicResolver topicResolver) {
		Cache cache = this.properties.getProducer().getCache();
		String defaultTopic = this.properties.getProducer().getTopicName();
		ProducerBuilderCustomizer<?> defaultConfigCustomizer = this.properties.getProducer()
			.toProducerBuilderCustomizer();
		return new CachingPulsarProducerFactory<>(pulsarClient, defaultTopic, defaultConfigCustomizer, topicResolver,
				cache.getExpireAfterAccess(), cache.getMaximumSize(), cache.getInitialCapacity());
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> interceptorsProvider, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		List<ProducerInterceptor> interceptors = interceptorsProvider.orderedStream().toList();
		Boolean observationEnabled = this.properties.getTemplate().isObservationsEnabled();
		return new PulsarTemplate<>(pulsarProducerFactory, interceptors, schemaResolver, topicResolver,
				observationEnabled);
	}

	@Bean
	@ConditionalOnMissingBean(SchemaResolver.class)
	DefaultSchemaResolver schemaResolver(
			Optional<SchemaResolverCustomizer<DefaultSchemaResolver>> schemaResolverCustomizer) {
		DefaultSchemaResolver schemaResolver = new DefaultSchemaResolver();
		List<TypeMapping> typeMappings = this.properties.getDefaults().getTypeMappings();
		if (typeMappings != null) {
			typeMappings.forEach((typeMapping) -> addCustomSchemaMapping(schemaResolver, typeMapping));
		}
		schemaResolverCustomizer.ifPresent((customizer) -> customizer.customize(schemaResolver));
		return schemaResolver;
	}

	private void addCustomSchemaMapping(DefaultSchemaResolver schemaResolver, TypeMapping typeMapping) {
		SchemaInfo schemaInfo = typeMapping.schemaInfo();
		if (schemaInfo != null) {
			Schema<Object> schema = schemaResolver
				.resolveSchema(schemaInfo.schemaType(), typeMapping.messageType(), schemaInfo.messageKeyType())
				.orElseThrow();
			schemaResolver.addCustomSchemaMapping(typeMapping.messageType(), schema);
		}
	}

	@Bean
	@ConditionalOnMissingBean(TopicResolver.class)
	DefaultTopicResolver topicResolver() {
		DefaultTopicResolver topicResolver = new DefaultTopicResolver();
		Defaults defaults = this.properties.getDefaults();
		if (defaults.getTypeMappings() != null) {
			defaults.getTypeMappings().forEach((typeMapping) -> addCustomTopicMapping(topicResolver, typeMapping));
		}
		return topicResolver;
	}

	private void addCustomTopicMapping(DefaultTopicResolver topicResolver, TypeMapping typeMapping) {
		String topicName = typeMapping.topicName();
		if (topicName != null) {
			topicResolver.addCustomTopicMapping(typeMapping.messageType(), topicName);
		}
	}

	@Bean
	@ConditionalOnMissingBean(PulsarConsumerFactory.class)
	DefaultPulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarConsumerFactory<>(pulsarClient,
				this.properties.getConsumer().toConsumerBuilderCustomizer());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.function.enabled", havingValue = "true", matchIfMissing = true)
	PulsarFunctionAdministration pulsarFunctionAdministration(PulsarAdministration pulsarAdministration,
			ObjectProvider<PulsarFunction> pulsarFunctions, ObjectProvider<PulsarSink> pulsarSinks,
			ObjectProvider<PulsarSource> pulsarSources) {
		PulsarProperties.Function function = this.properties.getFunction();
		return new PulsarFunctionAdministration(pulsarAdministration, pulsarFunctions, pulsarSinks, pulsarSources,
				function.getFailFast(), function.getPropagateFailures(), function.getPropagateStopFailures());
	}

	@Bean
	@ConditionalOnMissingBean(PulsarReaderFactory.class)
	DefaultPulsarReaderFactory<?> pulsarReaderFactory(PulsarClient pulsarClient) {
		return new DefaultPulsarReaderFactory<>(pulsarClient, this.properties.getReader().toReaderBuilderCustomizer());
	}

}
