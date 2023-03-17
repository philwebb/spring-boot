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

package org.springframework.boot.test.autoconfigure.service.connection;

import java.lang.annotation.Annotation;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.origin.OriginProvider;

/**
 * Interface passed to {@link ContainerConnectionDetailsFactory} to provide details of the
 * {@link GenericContainer} that provides the service.
 *
 * @param <A> the source annotation type. The annotation will mergable to a
 * {@link ServiceConnection @ServiceConnection}.
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see ContainerConnectionDetailsFactory
 */
public interface ServiceConnectedContainer<A extends Annotation> extends OriginProvider {

	Class<? extends ConnectionDetails> getConnectionDetailsType();

	/**
	 * Return the source annotation that provided the connection to the container. This
	 * annotation will be mergable to {@link ServiceConnection @ServiceConnection}.
	 * @return the source annotation
	 */
	A getAnnotation();

	/**
	 * Return the {@link GenericContainer} that implements the service being connected to.
	 * @return the {@link GenericContainer} providing the service
	 */
	GenericContainer<?> getContainer();

}
