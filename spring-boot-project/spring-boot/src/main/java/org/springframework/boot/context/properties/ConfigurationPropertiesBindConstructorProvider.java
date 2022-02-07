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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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
		Constructors constructors = Constructors.getConstructors(type);
		if (constructors.getBind() != null || isConstructorBindingType(type) || isNestedConstructorBinding) {
			Assert.state(constructors.getAutowired() == null,
					() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
		}
		return constructors.getBind();
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

	/**
	 * Data holder for autowired and bind constructors.
	 */
	static class Constructors {

		private final Constructor<?> autowired;

		private final Constructor<?> bind;

		private Constructors(Constructor<?> autowired, Constructor<?> bind) {
			this.autowired = autowired;
			this.bind = bind;
		}

		Constructor<?> getAutowired() {
			return this.autowired;
		}

		Constructor<?> getBind() {
			return this.bind;
		}

		static Constructors getConstructors(Class<?> type) {
			Constructor<?>[] candidates = getCandidateConstructors(type);
			Constructor<?> deducedBind = deduceBindConstructor(candidates);
			if (deducedBind != null) {
				return new Constructors(null, deducedBind);
			}
			Constructor<?> bind = findAnnotatedConstructor(type, candidates, ConstructorBinding.class, false);
			Constructor<?> autowired = findAnnotatedConstructor(type, candidates, Autowired.class, true);
			return new Constructors(autowired, bind);
		}

		private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
			if (isInnerClass(type)) {
				return type.getDeclaredConstructors();
			}
			return Arrays.stream(type.getDeclaredConstructors()).filter(Constructors::isNonSynthetic)
					.toArray(Constructor[]::new);
		}

		private static boolean isInnerClass(Class<?> type) {
			try {
				return type.getDeclaredField("this$0").isSynthetic();
			}
			catch (NoSuchFieldException ex) {
				return false;
			}
		}

		private static boolean isNonSynthetic(Constructor<?> constructor) {
			return !constructor.isSynthetic();
		}

		private static Constructor<?> deduceBindConstructor(Constructor<?>[] constructors) {
			if (constructors.length == 1 && constructors[0].getParameterCount() > 0 && !isAutowired(constructors[0])) {
				return constructors[0];
			}
			return null;
		}

		private static boolean isAutowired(Constructor<?> candidate) {
			return MergedAnnotations.from(candidate).isPresent(Autowired.class);
		}

		private static Constructor<?> findAnnotatedConstructor(Class<?> type, Constructor<?>[] candidates,
				Class<? extends Annotation> annotationType, boolean allowNoArgs) {
			Constructor<?> result = null;
			for (Constructor<?> candidate : candidates) {
				if (MergedAnnotations.from(candidate).isPresent(annotationType)) {
					Assert.state(allowNoArgs || candidate.getParameterCount() > 0, () -> type.getName() + " declares "
							+ ClassUtils.getShortName(annotationType) + " on a no-args constructor");
					Assert.state(result == null, () -> type.getName() + " has more than one "
							+ ClassUtils.getShortName(annotationType) + " constructor");
					result = candidate;
				}
			}
			return result;
		}

	}

}
