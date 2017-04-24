/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.Property;

/**
 * {@link BeanBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements BeanBinder {

	@Override
	public <T> T bind(Bindable<T> bindable, BeanPropertyBinder propertyBinder) {
		Bean<T> bean = Bean.get(bindable, propertyBinder.hasKnownBindableProperties());
		if (bean == null) {
			return null;
		}
		boolean bound = bind(bean, propertyBinder);
		return (bound ? bean.getInstance() : null);
	}

	private boolean bind(Bean<?> bean, BeanPropertyBinder propertyBinder) {
		boolean bound = false;
		for (Property property : bean.getProperties()) {
			Object boundValue = bind(bean, property, propertyBinder);
			bound |= boundValue != null;
		}
		return bound;
	}

	private Object bind(Bean<?> bean, Property property,
			BeanPropertyBinder propertyBinder) {
		ResolvableType type = getResolvableType(property);
		Supplier<Object> value = bean.getPropertyValue(property);
		String propertyName = BeanPropertyName.toDashedForm(property);
		Annotation[] annotations = getAnnotations(bean, property);
		Object bound = propertyBinder.bind(propertyName,
				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
		if (bound == null) {
			return null;
		}
		if (property.getWriteMethod() != null) {
			bean.setPropertyValue(property, bound);
		}
		else if (value == null || !bound.equals(value.get())) {
			throw new IllegalStateException(
					"No setter found for property: " + property.getName());
		}
		return bound;
	}

	private ResolvableType getResolvableType(Property property) {
		if (property.getWriteMethod() != null) {
			return ResolvableType.forMethodParameter(property.getWriteMethod(), 0);
		}
		else {
			return ResolvableType.forMethodReturnType(property.getReadMethod());
		}
	}

	private Annotation[] getAnnotations(Bean<?> bean, Property property) {
		try {
			Field field = bean.getType().getDeclaredField(property.getName());
			return field.getDeclaredAnnotations();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * The bean being bound.
	 */
	private static class Bean<T> {

		private final Class<?> type;

		private final Supplier<T> existingValue;

		private final List<Property> properties;

		private T instance;

		Bean(Class<?> type, Supplier<T> existingValue) {
			this.type = type;
			this.existingValue = existingValue;
			this.properties = convertToProperties(type,
					BeanUtils.getPropertyDescriptors(type));
		}

		private List<Property> convertToProperties(Class<?> type,
				PropertyDescriptor[] descriptors) {
			Stream<Property> properties = Arrays.stream(descriptors)
					.map((descriptor) -> convertToProperty(type, descriptor))
					.filter(p -> !isFiltered(p));
			return Collections.unmodifiableList(properties.collect(Collectors.toList()));
		}

		private Property convertToProperty(Class<?> type, PropertyDescriptor descriptor) {
			return new Property(type, descriptor.getReadMethod(),
					descriptor.getWriteMethod(), descriptor.getName());
		}

		private boolean isFiltered(Property property) {
			return "class".equals(property.getName());
		}

		public List<Property> getProperties() {
			return this.properties;
		}

		public Class<?> getType() {
			return this.type;
		}

		public Supplier<Object> getPropertyValue(Property property) {
			Method readMethod = property.getReadMethod();
			if (readMethod == null) {
				return null;
			}
			return () -> {
				try {
					readMethod.setAccessible(true);
					return readMethod.invoke(getInstance());
				}
				catch (Exception ex) {
					throw new IllegalStateException(
							"Unable to get value for property " + property.getName(), ex);
				}
			};
		}

		public void setPropertyValue(Property property, Object value) {
			try {
				Method writeMethod = property.getWriteMethod();
				writeMethod.setAccessible(true);
				writeMethod.invoke(getInstance(), value);
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Unable to set value for property " + property.getName(), ex);
			}
		}

		@SuppressWarnings("unchecked")
		public T getInstance() {
			if (this.instance == null) {
				if (this.existingValue != null) {
					this.instance = this.existingValue.get();
				}
				if (this.instance == null) {
					this.instance = (T) BeanUtils.instantiateClass(this.type);
				}
			}
			return this.instance;
		}

		public static <T> Bean<T> get(Bindable<T> bindable,
				boolean useExistingValueForType) {
			Class<?> type = bindable.getType().resolve();
			Supplier<T> value = bindable.getValue();
			if (value == null && (type.isInterface() || !hasDefaultConstructor(type))) {
				return null;
			}
			if (useExistingValueForType && value != null) {
				T instance = value.get();
				type = (instance != null ? instance.getClass() : type);
			}
			return new Bean<>(type, value);
		}

		private static boolean hasDefaultConstructor(Class<?> type) {
			return Arrays.stream(type.getDeclaredConstructors())
					.filter((c) -> c.getParameterCount() == 0).findFirst().isPresent();
		}

	}

}
