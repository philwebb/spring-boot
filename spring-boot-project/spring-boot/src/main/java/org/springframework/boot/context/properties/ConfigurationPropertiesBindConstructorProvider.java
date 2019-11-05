/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.BindConstructorProvider;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link BindConstructorProvider} used when binding
 * {@link ConfigurationProperties @ConfigurationProperties}.
 *
 * @author Madhura Bhave
 */
class ConfigurationPropertiesBindConstructorProvider implements BindConstructorProvider {

	public static final ConfigurationPropertiesBindConstructorProvider INSTANCE = new ConfigurationPropertiesBindConstructorProvider();

	@Override
	public Constructor<?> getBindConstructor(Class<?> type) {
		Constructor<?> bindConstructor = findBindConstructor(type);
		if (bindConstructor != null) {
			return bindConstructor;
		}
		if (isConstructorBindingType(type)) {
			Constructor<?> deducedConstructor = getDeducedConstructor(type);
			if (deducedConstructor != null) {
				return deducedConstructor;
			}
		}
		return null;
	}

	private static Constructor<?> getDeducedConstructor(Class<?> type) {
		if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
			if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
				return primaryConstructor;
			}
		}
		else {
			Constructor<?>[] constructors = type.getDeclaredConstructors();
			if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
				return constructors[0];
			}
		}
		return null;
	}

	private static boolean isConstructorBindingType(Class<?> type) {
		return MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES)
				.isPresent(ConstructorBinding.class);
	}

	private static Constructor<?> findBindConstructor(Class<?> type) {
		if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
			Constructor<?> constructor = BeanUtils.findPrimaryConstructor(type);
			if (constructor != null) {
				return findBindConstructor(type, constructor);
			}
		}
		return findBindConstructor(type, type.getDeclaredConstructors());
	}

	private static Constructor<?> findBindConstructor(Class<?> type, Constructor<?>... candidates) {
		Constructor<?> constructor = null;
		for (Constructor<?> candidate : candidates) {
			if (MergedAnnotations.from(candidate).isPresent(ConstructorBinding.class)) {
				Assert.state(candidate.getParameterCount() > 0,
						type.getName() + " declares @ConstructorBinding on a no-args constructor");
				Assert.state(constructor == null,
						type.getName() + " has more than one @ConstructorBinding constructor");
				constructor = candidate;
			}
		}
		return constructor;
	}

}
