/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Base for {@link OnBeanCondition} and {@link OnMissingBeanCondition}.
 *
 * @author Phillip Webb
 */
abstract class AbstractOnBeanCondition implements Condition {

	protected abstract Class<?> annotationClass();

	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				ConditionalOnMissingBean.class.getName(), true);
		List<String> beanClasses = collect(attributes, "value");
		List<String> beanNames = collect(attributes, "value");
		Assert.isTrue(beanClasses.size() > 0 || beanNames.size() > 0,
				"@" + ClassUtils.getShortName(annotationClass())
						+ " annotations must specify at least one bean");

		for (String beanClass : beanClasses) {
			try {
				String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						context.getBeanFactory(),
						ClassUtils.forName(beanClass, context.getClassLoader()), false,
						false);
				if (beans.length != 0) {
					return true;
				}
			}
			catch (ClassNotFoundException ex) {
			}
		}

		for (String beanName : beanNames) {
			if (context.getBeanFactory().containsBeanDefinition(beanName)) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> collect(MultiValueMap<String, Object> attributes, String key) {
		List<String> collected = new ArrayList<String>();
		List<String[]> valueList = (List) attributes.get(key);
		for (String[] valueArray : valueList) {
			for (String value : valueArray) {
				collected.add(value);
			}
		}
		return collected;
	}

}
