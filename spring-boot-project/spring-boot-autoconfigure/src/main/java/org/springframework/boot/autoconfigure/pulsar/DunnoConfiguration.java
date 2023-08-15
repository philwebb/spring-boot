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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.pulsar.config.DefaultPulsarReaderContainerFactory;
import org.springframework.pulsar.core.PulsarReaderFactory;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.reader.PulsarReaderContainerProperties;

/**
 * @author pwebb
 */
public class DunnoConfiguration {

	// XPulsarAutoConfiguration

	// XPulsarAnnotationDrivenConfiguration

	@Bean
	@ConditionalOnMissingBean(name = "pulsarReaderContainerFactory")
	DefaultPulsarReaderContainerFactory<?> pulsarReaderContainerFactory(
			ObjectProvider<PulsarReaderFactory<Object>> readerFactoryProvider, SchemaResolver schemaResolver) {
		PulsarReaderContainerProperties reader = null; // FIXME
		return new DefaultPulsarReaderContainerFactory<>(readerFactoryProvider.getIfAvailable(), reader);
	}

}
