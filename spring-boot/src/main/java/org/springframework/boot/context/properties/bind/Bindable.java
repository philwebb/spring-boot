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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Source that can be bound by a {@link Binder}.
 *
 * @param <T> The source type
 * @author Philip Webb
 * @since 2.0.0
 * @see Bindable#of(Class)
 * @see Bindable#of(ResolvableType)
 */
public final class Bindable<T> {

	private static final Annotation[] NO_ANNOTATIONS = {};

	private final ResolvableType type;

	private final ResolvableType boxedType;

	private final Supplier<T> existingValue;

	private final Annotation[] annotations;

	private final Object defaultValue;

	private Bindable(ResolvableType type, ResolvableType boxedType,
			Supplier<T> existingValue, Annotation[] annotations, Object defaultValue) {
		this.type = type;
		this.boxedType = boxedType;
		this.existingValue = existingValue;
		this.annotations = annotations;
		this.defaultValue = defaultValue;
	}

	/**
	 * Return the type of the item to bind.
	 * @return the type being bound
	 */
	public ResolvableType getType() {
		return this.type;
	}

	public ResolvableType getBoxedType() {
		return this.boxedType;
	}

	/**
	 * Return the existing object value or {@code null}.
	 * @return the existing value
	 */
	public Supplier<T> getExistingValue() {
		return this.existingValue;
	}

	/**
	 * Return any associated annotations that could affect binding.
	 * @return the associated annotations
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Return the default value to be returned if binding fails.
	 * @return the default value or {@code null}
	 */
	public Object getDefaultValue() {
		return this.defaultValue;
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("type", this.type);
		creator.append("existingValue",
				(this.existingValue == null ? "none" : "provided"));
		creator.append("annotations", this.annotations);
		creator.append("defaultValue", this.defaultValue);
		return creator.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.type);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.annotations);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.defaultValue);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Bindable<?> other = (Bindable<?>) obj;
		boolean result = true;
		result = result && nullSafeEquals(this.type.resolve(), other.type.resolve());
		result = result && nullSafeEquals(this.annotations, other.annotations);
		result = result && nullSafeEquals(this.defaultValue, other.defaultValue);
		return result;
	}

	private boolean nullSafeEquals(Object o1, Object o2) {
		return ObjectUtils.nullSafeEquals(o1, o2);
	}

	/**
	 * Create an updated {@link Bindable} instance with the specified annotations.
	 * @param annotations the annotations
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withAnnotations(Annotation... annotations) {
		return new Bindable<T>(this.type, this.boxedType, this.existingValue, annotations,
				this.defaultValue);
	}

	/**
	 * Create an updated {@link Bindable} instance with the specified default value.
	 * @param defaultValue the default value or {@code null}
	 * @return an updated {@link Bindable}
	 */
	public Bindable<T> withDefaultValue(Object defaultValue) {
		return new Bindable<T>(this.type, this.boxedType, this.existingValue,
				this.annotations, defaultValue);
	}

	/**
	 * Create a new {@link Bindable} of the specified type with an existing value equal to
	 * a new instance of that type.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null} and must have a default constructor)
	 * @return a {@link Bindable} instance
	 * @see #of(Class, Object)
	 */
	@Deprecated
	public static <T> Bindable<T> ofNewInstance(Class<T> type) {
		Assert.notNull(type, "Type must not be null");
		Assert.state(hasDefaultConstructor(type), "Type must have default constructor");
		T instance = BeanUtils.instantiateClass(type);
		return of(type, instance);
	}

	private static boolean hasDefaultConstructor(Class<?> type) {
		return Arrays.stream(type.getDeclaredConstructors())
				.anyMatch((c) -> c.getParameterCount() == 0);
	}

	/**
	 * Create a new {@link Bindable} of the type of the specified instance with an
	 * existing value equal to the instance.
	 * @param <T> The source type
	 * @param instance the instance (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(Class, Object)
	 */
	public static <T> Bindable<T> ofInstance(T instance) {
		Assert.notNull(instance, "Instance must not be null");
		return of(instance.getClass(), instance);
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(Class, Object)
	 * @see #of(ResolvableType)
	 */
	public static <T> Bindable<T> of(Class<T> type) {
		return of(type, null);
	}

	/**
	 * Create a new {@link Bindable} of the specified type.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType, Object)
	 * @see #of(Class)
	 */
	public static <T> Bindable<T> of(ResolvableType type) {
		return of(type, null);
	}

	/**
	 * Create a new {@link Bindable} of the specified type with an existing value.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @param existingValue the existing value (may be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType, Supplier)
	 */
	public static <T> Bindable<T> of(Class<?> type, T existingValue) {
		Assert.notNull(type, "Type must not be null");
		Supplier<T> supplier = (existingValue == null ? null : () -> existingValue);
		return of(ResolvableType.forClass(type), supplier, true);
	}

	/**
	 * Create a new {@link Bindable} of the specified type with an existing value.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @param existingValue the existing value (may be {@code null})
	 * @return a {@link Bindable} instance
	 * @see #of(ResolvableType, Supplier)
	 */
	public static <T> Bindable<T> of(Class<?> type, Supplier<T> existingValue) {
		Assert.notNull(type, "Type must not be null");
		return of(ResolvableType.forClass(type), existingValue, false);
	}

	/**
	 * Create a new {@link Bindable} of the specified type with an existing value.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @param existingValue the existing value (may be {@code null})
	 * @return a {@link Bindable} instance
	 */
	public static <T> Bindable<T> of(ResolvableType type, T existingValue) {
		Assert.notNull(type, "Type must not be null");
		Supplier<T> supplier = (existingValue == null ? null : () -> existingValue);
		return of(type, supplier, true);
	}

	/**
	 * Create a new {@link Bindable} of the specified type with an existing value.
	 * @param <T> The source type
	 * @param type the type (must not be {@code null})
	 * @param existingValue the existing value (may be {@code null})
	 * @return a {@link Bindable} instance
	 */
	public static <T> Bindable<T> of(ResolvableType type, Supplier<T> existingValue) {
		Assert.notNull(type, "Type must not be null");
		return of(type, existingValue, false);
	}

	private static <T> Bindable<T> of(ResolvableType type, Supplier<T> existingValue,
			boolean checkType) {
		ResolvableType boxedType = box(type);
		if (checkType && existingValue != null) {
			T instance = existingValue.get();
			Assert.isTrue(
					instance == null || type.isArray()
							|| boxedType.resolve().isInstance(instance),
					"ExistingValue must be an instance of " + type);
		}
		return new Bindable<>(type, boxedType, existingValue, NO_ANNOTATIONS, null);
	}

	private static ResolvableType box(ResolvableType type) {
		Class<?> resolved = type.resolve();
		if (resolved != null && resolved.isPrimitive()) {
			Object array = Array.newInstance(resolved, 1);
			Class<?> wrapperType = Array.get(array, 0).getClass();
			return ResolvableType.forClass(wrapperType);
		}
		if (resolved.isArray()) {
			return ResolvableType.forArrayComponent(box(type.getComponentType()));
		}
		return type;
	}

}
