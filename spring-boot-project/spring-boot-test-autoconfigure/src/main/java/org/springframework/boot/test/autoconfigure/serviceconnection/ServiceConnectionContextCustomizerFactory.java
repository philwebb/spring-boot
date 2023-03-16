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

package org.springframework.boot.test.autoconfigure.serviceconnection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnectionSource;
import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizerFactory} to support service connections in tests.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceConnectionContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	@SuppressWarnings("unchecked")
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		List<ServiceConnectionSource<?, ?>> sources = new ArrayList<>();
		ReflectionUtils.doWithFields(testClass, (field) -> {
			MergedAnnotations annotations = MergedAnnotations.from(field);
			sources.addAll(annotations.stream(ConnectableService.class)
				.map((connectableService) -> (Class<? extends ServiceConnection>) connectableService.getClass("value"))
				.map((connectionType) -> createSource(field, connectionType))
				.toList());
		}, (field) -> GenericContainer.class.isAssignableFrom(field.getType()));
		return (sources.isEmpty()) ? null : new ServiceConnectionContextCustomizer(sources);
	}

	private ServiceConnectionSource<?, ?> createSource(Field field, Class<? extends ServiceConnection> connectionType) {
		ReflectionUtils.makeAccessible(field);
		Object input = ReflectionUtils.getField(field, null);
		return new ServiceConnectionSource<>(input, field.getName() + connectionType.getSimpleName(),
				new FieldOrigin(field), connectionType);
	}

	private static class FieldOrigin implements Origin {

		private final Field field;

		FieldOrigin(Field field) {
			this.field = field;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			FieldOrigin other = (FieldOrigin) obj;
			return Objects.equals(this.field, other.field);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.field);
		}

		@Override
		public String toString() {
			return this.field.toString();
		}

	}

}
