/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.context.annotation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConditionPurpose;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Base for {@link OnBeanCondition} and {@link OnMissingBeanCondition}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
abstract class AbstractOnBeanCondition implements Condition {

	protected Log logger = LogFactory.getLog(getClass());

	protected abstract Class<?> annotationClass();

	@Override
	public boolean matches(ConditionContext context, ConditionPurpose purpose,
			AnnotatedTypeMetadata metadata) {

		if (purpose != ConditionPurpose.REGISTER_BEAN) {
			return true;
		}

		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				annotationClass().getName(), true);
		List<String> beanClasses = collect(attributes, "value");
		List<String> beanNames = collect(attributes, "name");
		Assert.isTrue(beanClasses.size() > 0 || beanNames.size() > 0,
				"@" + ClassUtils.getShortName(annotationClass())
						+ " annotations must specify at least one bean");
		return matches(context, purpose, metadata, beanClasses, beanNames);
	}

	protected boolean matches(ConditionContext context, ConditionPurpose purpose,
			AnnotatedTypeMetadata metadata, List<String> beanClasses,
			List<String> beanNames) throws LinkageError {

		String checking = ConditionLogUtils.getPrefix(this.logger, metadata);

		Boolean considerHierarchy = (Boolean) metadata.getAnnotationAttributes(
				annotationClass().getName()).get("considerHierarchy");
		considerHierarchy = (considerHierarchy == null ? false : considerHierarchy);

		List<String> beanClassesFound = new ArrayList<String>();
		List<String> beanNamesFound = new ArrayList<String>();

		for (String beanClass : beanClasses) {
			try {
				// eagerInit set to false to prevent early instantiation (some
				// factory beans will not be able to determine their object type at this
				// stage, so those are not eligible for matching this condition)
				ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
				Class<?> type = ClassUtils.forName(beanClass, context.getClassLoader());
				String[] beans = (considerHierarchy ? BeanFactoryUtils
						.beanNamesForTypeIncludingAncestors(beanFactory, type, false,
								false) : beanFactory.getBeanNamesForType(type, false,
						false));
				if (beans.length != 0) {
					beanClassesFound.add(beanClass);
				}
			} catch (ClassNotFoundException ex) {
			}
		}
		for (String beanName : beanNames) {
			if (considerHierarchy ? context.getBeanFactory().containsBean(beanName)
					: context.getBeanFactory().containsLocalBean(beanName)) {
				beanNamesFound.add(beanName);
			}
		}

		boolean result = evaluate(beanClassesFound, beanNamesFound);
		if (this.logger.isDebugEnabled()) {
			logFoundResults(checking, "class", beanClasses, beanClassesFound);
			logFoundResults(checking, "name", beanNames, beanClassesFound);
			this.logger.debug(checking + "Match result is: " + result);
		}
		return result;
	}

	private void logFoundResults(String prefix, String type, List<?> candidates,
			List<?> found) {
		if (!candidates.isEmpty()) {
			this.logger.debug(prefix + "Looking for beans with " + type + ": "
					+ candidates);
			if (found.isEmpty()) {
				this.logger.debug(prefix + "Found no beans");
			} else {
				this.logger.debug(prefix + "Found beans with " + type + ": " + found);
			}
		}
	}

	protected boolean evaluate(List<String> beanClassesFound, List<String> beanNamesFound) {
		return !beanClassesFound.isEmpty() || !beanNamesFound.isEmpty();
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
