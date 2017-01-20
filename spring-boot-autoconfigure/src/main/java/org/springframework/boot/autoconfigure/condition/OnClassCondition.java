/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} and {@link AutoConfigurationImportFilter} that checks for the
 * presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class OnClassCondition extends SpringBootCondition
		implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {

	private final String preparedOnClassProperties;

	private BeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	OnClassCondition() {
		this(ConditionalOnClass.class.getName());
	}

	OnClassCondition(String preparedOnClassProperties) {
		this.preparedOnClassProperties = "META-INF/" + preparedOnClassProperties
				+ ".properties";
	}

	@Override
	public boolean[] match(String[] autoConfigurationClasses) {
		ConditionEvaluationReport report = getConditionEvaluationReport();
		ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses);
		boolean[] match = new boolean[outcomes.length];
		for (int i = 0; i < outcomes.length; i++) {
			match[i] = (outcomes[i] == null || outcomes[i].isMatch());
			if (!match[i] && outcomes[i] != null) {
				logOutcome(autoConfigurationClasses[i], outcomes[i]);
				if (report != null) {
					report.recordConditionEvaluation(autoConfigurationClasses[i], this,
							outcomes[i]);
				}
			}
		}
		return match;
	}

	private ConditionEvaluationReport getConditionEvaluationReport() {
		if (this.beanFactory != null
				&& this.beanFactory instanceof ConfigurableBeanFactory) {
			return ConditionEvaluationReport
					.get((ConfigurableListableBeanFactory) this.beanFactory);
		}
		return null;
	}

	private ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses) {
		MultiValueMap<String, String> conditions = getProcessedOnClassConditions();
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		// Split the work and perform half in a background thread. Using a single
		// additional thread seems to offer the best performance. More threads make things
		// worse
		int split = outcomes.length / 2;
		Thread thread = new Thread(
				fillOutcomes(autoConfigurationClasses, conditions, outcomes, 0, split));
		thread.start();
		fillOutcomes(autoConfigurationClasses, conditions, outcomes, split,
				outcomes.length).run();
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return outcomes;
	}

	private MultiValueMap<String, String> getProcessedOnClassConditions() {
		try {
			Enumeration<URL> urls = (this.beanClassLoader != null
					? this.beanClassLoader.getResources(this.preparedOnClassProperties)
					: ClassLoader.getSystemResources(this.preparedOnClassProperties));
			MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>();
			while (urls.hasMoreElements()) {
				addProperties(result, urls.nextElement());
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Unable to load @ConditionalOnClass location ["
							+ this.preparedOnClassProperties + "]",
					ex);
		}
	}

	private void addProperties(MultiValueMap<String, String> result, URL url)
			throws IOException {
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new UrlResource(url));
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			for (String onClass : StringUtils
					.commaDelimitedListToStringArray((String) entry.getValue())) {
				result.add((String) entry.getKey(), onClass);
			}
		}
	}

	private Runnable fillOutcomes(final String[] autoConfigurationClasses,
			final MultiValueMap<String, String> conditions,
			final ConditionOutcome[] outcomes, final int start, final int end) {
		return new Runnable() {

			@Override
			public void run() {
				for (int i = start; i < end; i++) {
					List<String> onClasses = conditions.get(autoConfigurationClasses[i]);
					fillOutcome(onClasses, outcomes, i);
				}
			}

		};
	}

	private void fillOutcome(List<String> onClasses, ConditionOutcome[] outcomes,
			int index) {
		try {
			List<String> missing = getMatches(onClasses, MatchType.MISSING,
					this.beanClassLoader);
			if (!missing.isEmpty()) {
				outcomes[index] = ConditionOutcome
						.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
								.didNotFind("required class", "required classes")
								.items(Style.QUOTE, missing));
			}
		}
		catch (Exception ex) {
			// We'll get another chance later
			outcomes[index] = null;
		}
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ClassLoader classLoader = context.getClassLoader();
		ConditionMessage matchMessage = ConditionMessage.empty();
		List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
		if (onClasses != null) {
			List<String> missing = getMatches(onClasses, MatchType.MISSING, classLoader);
			if (!missing.isEmpty()) {
				return ConditionOutcome
						.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
								.didNotFind("required class", "required classes")
								.items(Style.QUOTE, missing));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
					.found("required class", "required classes").items(Style.QUOTE,
							getMatches(onClasses, MatchType.PRESENT, classLoader));
		}
		List<String> onMissingClasses = getCandidates(metadata,
				ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			List<String> present = getMatches(onMissingClasses, MatchType.PRESENT,
					classLoader);
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(
						ConditionMessage.forCondition(ConditionalOnMissingClass.class)
								.found("unwanted class", "unwanted classes")
								.items(Style.QUOTE, present));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
					.didNotFind("unwanted class", "unwanted classes").items(Style.QUOTE,
							getMatches(onMissingClasses, MatchType.MISSING, classLoader));
		}
		return ConditionOutcome.match(matchMessage);
	}

	private List<String> getCandidates(AnnotatedTypeMetadata metadata,
			Class<?> annotationType) {
		MultiValueMap<String, Object> attributes = metadata
				.getAllAnnotationAttributes(annotationType.getName(), true);
		List<String> candidates = new ArrayList<String>();
		if (attributes == null) {
			return Collections.emptyList();
		}
		addAll(candidates, attributes.get("value"));
		addAll(candidates, attributes.get("name"));
		return candidates;
	}

	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private List<String> getMatches(Collection<String> candiates, MatchType matchType,
			ClassLoader classLoader) {
		List<String> matches = new ArrayList<String>(candiates.size());
		for (String candidate : candiates) {
			if (matchType.matches(candidate, classLoader)) {
				matches.add(candidate);
			}
		}
		return matches;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	private enum MatchType {

		PRESENT {

			@Override
			public boolean matches(String className, ClassLoader classLoader) {
				return isPresent(className, classLoader);
			}

		},

		MISSING {

			@Override
			public boolean matches(String className, ClassLoader classLoader) {
				return !isPresent(className, classLoader);
			}

		};

		private static boolean isPresent(String className, ClassLoader classLoader) {
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			try {
				forName(className, classLoader);
				return true;
			}
			catch (Throwable ex) {
				return false;
			}
		}

		private static Class<?> forName(String className, ClassLoader classLoader)
				throws ClassNotFoundException {
			if (classLoader != null) {
				return classLoader.loadClass(className);
			}
			return Class.forName(className);
		}

		public abstract boolean matches(String className, ClassLoader classLoader);

	}

}
