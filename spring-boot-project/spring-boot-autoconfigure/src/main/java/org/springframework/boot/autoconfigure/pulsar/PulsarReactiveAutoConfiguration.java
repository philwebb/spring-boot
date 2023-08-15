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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.config.PulsarAnnotationSupportBeanNames;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.config.DefaultReactivePulsarListenerContainerFactory;
import org.springframework.pulsar.reactive.config.annotation.EnableReactivePulsar;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;

/**
 * @author pwebb
 */
@Import(PulsarConfiguration.class)
public class PulsarReactiveAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "reactivePulsarListenerContainerFactory")
	DefaultReactivePulsarListenerContainerFactory<?> reactivePulsarListenerContainerFactory(
			ObjectProvider<ReactivePulsarConsumerFactory<Object>> consumerFactoryProvider,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return null; // XPulsarReactiveAnnotationDrivenConfiguration
	}

	@Bean
	@ConditionalOnMissingBean
	PulsarTemplate<?> pulsarTemplate(PulsarProducerFactory<?> pulsarProducerFactory,
			ObjectProvider<ProducerInterceptor> interceptorsProvider, SchemaResolver schemaResolver,
			TopicResolver topicResolver) {
		return null;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "true", matchIfMissing = true)
	CachingPulsarProducerFactory<?> cachingPulsarProducerFactory(PulsarClient pulsarClient,
			TopicResolver topicResolver) {
		return null;
	}

	@Bean
	@ConditionalOnMissingBean(PulsarProducerFactory.class)
	@ConditionalOnProperty(name = "spring.pulsar.producer.cache.enabled", havingValue = "false")
	DefaultPulsarProducerFactory<?> pulsarProducerFactory(PulsarClient pulsarClient, TopicResolver topicResolver) {
		// FIXME merge these to save two methods?
		return null;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableReactivePulsar
	@ConditionalOnMissingBean(
			name = PulsarAnnotationSupportBeanNames.REACTIVE_PULSAR_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableReactivePulsarConfiguration {

	}

}
