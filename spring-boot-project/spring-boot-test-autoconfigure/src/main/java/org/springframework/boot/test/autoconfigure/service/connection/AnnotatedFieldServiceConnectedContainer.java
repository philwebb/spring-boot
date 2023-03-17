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
import java.lang.reflect.Field;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * {@link ServiceConnectedContainer} backed by an annotated field.
 *
 * @param <A> the source annotation type.
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class AnnotatedFieldServiceConnectedContainer<A extends Annotation> implements ServiceConnectedContainer<A> {

	private final Class<? extends ConnectionDetails> connectionDetailsType;

	private final A annotation;

	private final GenericContainer<?> container;

	private final AnnotatedFieldOrigin origin;

	@SuppressWarnings("unchecked")
	AnnotatedFieldServiceConnectedContainer(Class<? extends ConnectionDetails> connectionDetailsType, Field field,
			MergedAnnotation<ServiceConnection> annotation, GenericContainer<?> container) {
		this(connectionDetailsType, field, (A) annotation.getRoot().synthesize(), container);
	}

	AnnotatedFieldServiceConnectedContainer(Class<? extends ConnectionDetails> connectionDetailsType, Field field,
			A annotation, GenericContainer<?> container) {
		this.connectionDetailsType = connectionDetailsType;
		this.annotation = annotation;
		this.container = container;
		this.origin = new AnnotatedFieldOrigin(field, annotation);
	}

	@Override
	public Class<? extends ConnectionDetails> getConnectionDetailsType() {
		return this.connectionDetailsType;
	}

	@Override
	public A getAnnotation() {
		return this.annotation;
	}

	@Override
	public GenericContainer<?> getContainer() {
		return this.container;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

}
