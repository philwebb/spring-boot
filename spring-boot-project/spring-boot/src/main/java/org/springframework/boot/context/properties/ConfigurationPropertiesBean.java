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

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

/**
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class ConfigurationPropertiesBean {

	public ResolvableType getType() {
		return null;
	}

	public Annotation[] getAnnotations() {
		return null;
	}

	public String getBeanName() {
		return null;
	}

	public Object getBean() {
		return null;
	}

	public static boolean isConfigurationPropertiesBean(ApplicationContext applicationContext, String beanName,
			Object bean) {
		return false;
	}

	public static ConfigurationPropertiesBean get(ApplicationContext applicationContext, String beanName, Object bean) {
		return null;
	}

	public static ConfigurationPropertiesBean forValueObject(Class<?> type, String beanName) {
		return null;
	}

	public static Map<String, ConfigurationPropertiesBean> getAll(ApplicationContext applicationContext) {
		return null;
	}

	/**
	 * @param type
	 * @return
	 */
	public static boolean isValueObject(Class<?> type) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}
	// private static boolean isValueObject(Class<?> type) {
	// List<Constructor<?>> constructors = determineConstructors(type);
	// return (constructors.size() == 1 && constructors.get(0).getParameterCount() > 0);
	// }
	//
	// private static List<Constructor<?>> determineConstructors(Class<?> type) {
	// List<Constructor<?>> constructors = new ArrayList<>();
	// if (KOTLIN_PRESENT && KotlinDetector.isKotlinType(type)) {
	// Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
	// if (primaryConstructor != null) {
	// constructors.add(primaryConstructor);
	// }
	// }
	// else {
	// constructors.addAll(Arrays.asList(type.getDeclaredConstructors()));
	// }
	// return constructors;
	// }

	/**
	 * @return
	 */
	Bindable<?> asBindable() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}
	// private static final boolean KOTLIN_PRESENT = KotlinDetector.isKotlinPresent();

}
