/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.List;

import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * Invokes the available {@link GrpcChannelBuilderCustomizer} instances for a given
 * {@link ManagedChannelBuilder}.
 *
 * @author Chris Bono
 */
class ChannelBuilderCustomizers {

	private ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers;

	ChannelBuilderCustomizers(ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers) {
		this.customizers = customizers;
	}

	<T extends ManagedChannelBuilder<T>> List<GrpcChannelBuilderCustomizer<T>> forFactory() {
		return List.of(this::customize);
	}

	@SuppressWarnings("unchecked")
	<T extends ManagedChannelBuilder<T>> void customize(String target, T builder) {
		LambdaSafe.callbacks(GrpcChannelBuilderCustomizer.class, this.customizers.orderedStream().toList(), builder)
			.withLogger(ChannelBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(target, builder));
	}

}