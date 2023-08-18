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

import org.springframework.pulsar.core.ConsumerBuilderCustomizer;
import org.springframework.pulsar.core.ProducerBuilderCustomizer;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;

/**
 * Helper class used to map {@link PulsarProperties}.
 *
 * @author Chris Bono
 */
class PulsarPropertyMapper {

	/**
	 * @param properties
	 * @return
	 */
	public static PulsarClientBuilderCustomizer clientCustomizer(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param producer
	 * @return
	 */
	public static ProducerBuilderCustomizer<?> producerBuilderCustomizer(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param properties
	 * @return
	 */
	public static ConsumerBuilderCustomizer<?> consumerBuilderCustomizer(PulsarProperties properties) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
