/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

	static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

	@Override
	public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
		return getBindConstructor(bindable.getType().resolve(), isNestedConstructorBinding);
	}

	Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
		if (type == null) {
			return null;
		}
		Constructors constructors = getConstructors(type);
		if (constructors.getBind() != null || isConstructorBindingType(type) || isNestedConstructorBinding) {
			Assert.state(constructors.getAutowired() == null,
					() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
		}
		return constructors.getBind();
	}

	private Constructors getConstructors(Class<?> type) {
		Constructor<?> constructor = null;
		Constructor<?> autowiredConstructor = null;
		Constructor<?>[] candidates = Arrays.stream(type.getDeclaredConstructors())
				.filter((candidate) -> !isSynthetic(candidate, type)).toArray(Constructor[]::new);
		Constructor<?> bindConstructor = deduceBindConstructor(candidates);
		if (bindConstructor != null) {
			return new Constructors(null, bindConstructor);
		}
		for (Constructor<?> candidate : candidates) {
			if (MergedAnnotations.from(candidate).isPresent(Autowired.class)) {
				autowiredConstructor = candidate;
				continue;
			}
			constructor = findAnnotatedConstructor(type, constructor, candidate);
		}
		return new Constructors(autowiredConstructor, constructor);
	}

	private boolean isSynthetic(Constructor<?> candidate, Class<?> type) {
		if (candidate.isSynthetic()) {
			return true;
		}
		try {
			Field field = type.getDeclaredField("this$0");
			if (field.isSynthetic()) {
				return true;
			}
		}
		catch (NoSuchFieldException ex) {
		}
		return false;
	}

	private Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?> constructor,
			Constructor<?> candidate) {
		if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
			Assert.state(candidate.getParameterCount() > 0,
					() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
			Assert.state(constructor == null,
					() -> type.getName() + " has more than one @ConstructorBinding constructor");
			constructor = candidate;
		}
		return constructor;
	}

	private boolean isConstructorBindingType(Class<?> type) {
		return isImplicitConstructorBindingType(type) || isConstructorBindingAnnotatedType(type);
	}

	private boolean isImplicitConstructorBindingType(Class<?> type) {
		Class<?> superclass = type.getSuperclass();
		return (superclass != null) && "java.lang.Record".equals(superclass.getName());
	}

	private boolean isConstructorBindingAnnotatedType(Class<?> type) {
		return MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
				.isPresent(ConstructorBinding.class);
	}

	private Constructor<?> deduceBindConstructor(Constructor<?>[] constructors) {
		if (constructors.length == 1 && constructors[0].getParameterCount() > 0
				&& !MergedAnnotations.from(constructors[0]).isPresent(Autowired.class)) {
			return constructors[0];
		}
		return null;
	}

	/**
	 * Data holder for autowired and bind constructors.
	 */
	static class Constructors {

		private final Constructor<?> autowired;

		private final Constructor<?> bind;

		Constructors(Constructor<?> autowired, Constructor<?> bind) {
			this.autowired = autowired;
			this.bind = bind;
		}

		Constructor<?> getAutowired() {
			return this.autowired;
		}

		Constructor<?> getBind() {
			return this.bind;
		}

	}

}
