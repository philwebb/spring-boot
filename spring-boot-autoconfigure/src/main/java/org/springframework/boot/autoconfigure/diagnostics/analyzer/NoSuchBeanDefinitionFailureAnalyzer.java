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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.Opcodes;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.ConsumerDescriptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link NoSuchBeanDefinitionException}.
 *
 * @author Stephane Nicoll
 */
class NoSuchBeanDefinitionFailureAnalyzer
		extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
		implements BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private MetadataReaderFactory metadataReaderFactory;

	private ConditionEvaluationReport report;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.metadataReaderFactory = new CachingMetadataReaderFactory(
				this.beanFactory.getBeanClassLoader());
		// The report must be lazily retrieved as it won't be accessible once
		// the context has failed to start
		this.report = ConditionEvaluationReport.get(this.beanFactory);
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
		if (cause.getNumberOfBeansFound() != 0) {
			return null;
		}
		List<NoMatchDescriptionProvider> providers = extractNoMatchDescriptionProviders(
				new BeanCandidateMethodMetadataFilter(this.beanFactory.getBeanClassLoader(), cause));
		StringBuilder message = new StringBuilder();
		String consumerDescription = ConsumerDescriptionResolver
				.getConsumerDescription(rootFailure);
		if (consumerDescription != null) {
			message.append(consumerDescription);
		}
		else {
			message.append("A component");
		}
		message.append(finalizeMessage(cause));

		if (providers.isEmpty()) {
			message.append(String.format("\tNo matching auto-configuration has been found for this type.%n"));
		}
		else {
			for (NoMatchDescriptionProvider provider : providers) {
				message.append(String.format("\t- %s%n", provider.generateNoMatchDescription()));
			}
		}
		return new FailureAnalysis(message.toString(),
				generateAction(cause, !providers.isEmpty()), cause);
	}

	private String finalizeMessage(NoSuchBeanDefinitionException ex) {
		if (ex.getBeanType() != null) {
			return String.format(" required a bean of type '%s' that could not be found:%n", ex.getBeanType().getName());
		}
		else {
			return String.format(" required a bean named '%s' that could not be found:%n", ex.getBeanName());
		}
	}

	private String generateAction(NoSuchBeanDefinitionException ex, boolean potentialMatch) {
		StringBuilder sb = new StringBuilder("Consider ");
		if (potentialMatch) {
			sb.append("revisiting the conditions above or ");
		}
		if (ex.getBeanType() != null) {
			sb.append("defining a bean of type '%s' in your configuration.");
			return String.format(sb.toString(), ex.getBeanType().getName());
		}
		else {
			sb.append("defining a bean named '%s' in your configuration.");
			return String.format(sb.toString(), ex.getBeanName());
		}
	}


	private List<NoMatchDescriptionProvider> extractNoMatchDescriptionProviders(MethodMetadataFilter filter) {
		List<NoMatchDescriptionProvider> result = new ArrayList<NoMatchDescriptionProvider>();
		for (Map.Entry<String, ConditionEvaluationReport.ConditionAndOutcomes> entry :
				this.report.getConditionAndOutcomesBySource().entrySet()) {
			if (!entry.getValue().isFullMatch()) {
				String source = entry.getKey();
				boolean methodEvaluated = isMethod(source);
				List<MethodMetadata> methods = extractBeansMethodMetadata(source, filter);
				if (methods != null) {
					for (ConditionEvaluationReport.ConditionAndOutcome cao : entry.getValue()) {
						if (!cao.getOutcome().isMatch()) {
							for (MethodMetadata method : methods) {
								result.add(new NoMatchDescriptionProvider(method, cao.getOutcome(), methodEvaluated));
							}
						}
					}
				}
			}
		}
		for (String excludedClass : this.report.getExclusions()) {
			List<MethodMetadata> methods = extractBeansMethodMetadata(excludedClass, filter);
			if (methods != null) {
				for (MethodMetadata method : methods) {
					String message = String.format("auto-configuration %s was excluded", excludedClass);
					result.add(new NoMatchDescriptionProvider(method, new ConditionOutcome(false, message), false));
				}
			}
		}
		return result;
	}


	private List<MethodMetadata> extractBeansMethodMetadata(String source, MethodMetadataFilter filter) {
		String[] tokens = source.split("#");
		try {
			String className = (tokens.length > 1 ? tokens[0] : source);
			String methodName = (tokens.length == 2 ? tokens[1] : null);
			MetadataReader configurationClass = this.metadataReaderFactory.getMetadataReader(className);
			return findBeanMethods(configurationClass.getAnnotationMetadata(), filter, methodName);
		}
		catch (Exception ex) {
			return null;
		}
	}

	private boolean isMethod(String source) {
		return source.split("#").length == 2;
	}

	private List<MethodMetadata> findBeanMethods(AnnotationMetadata classMetadata, MethodMetadataFilter filter, String name) {
		Set<MethodMetadata> beanMethods = classMetadata.getAnnotatedMethods(Bean.class.getName());
		List<MethodMetadata> result = new ArrayList<MethodMetadata>();
		for (MethodMetadata beanMethod : beanMethods) {
			if ((name == null || beanMethod.getMethodName().equals(name)) && filter.matches(beanMethod)) {
				result.add(beanMethod);
			}
		}
		return result;
	}

	private interface MethodMetadataFilter {

		boolean matches(MethodMetadata methodMetadata);

	}

	private static final class BeanCandidateMethodMetadataFilter implements MethodMetadataFilter {

		private final ClassLoader classloader;

		private final String beanName;

		private final Class<?> beanType;

		private BeanCandidateMethodMetadataFilter(ClassLoader classloader,
				NoSuchBeanDefinitionException exception) {
			this.classloader = classloader;
			this.beanName = exception.getBeanName();
			this.beanType = exception.getBeanType();
		}

		@Override
		public boolean matches(MethodMetadata methodMetadata) {
			if (!isPublic(methodMetadata)) {
				return false;
			}
			if (this.beanName != null && hasBeanName(methodMetadata)) {
				return true;
			}
			if (this.beanType != null && isAssignable(this.beanType, methodMetadata.getReturnTypeName())) {
				return true;
			}
			return false;
		}

		private boolean hasBeanName(MethodMetadata methodMetadata) {
			Map<String, Object> attributes = methodMetadata.getAnnotationAttributes(Bean.class.getName());
			if (attributes != null) {
				String[] candidates = (String[]) attributes.get("name");
				if (candidates != null) {
					for (String candidate : candidates) {
						if (candidate.equals(this.beanName)) {
							return true;
						}
					}
				}
			}
			if (methodMetadata.getMethodName().equals(this.beanName)) {
				return true;
			}
			return false;
		}

		private boolean isAssignable(Class<?> beanType, String type) {
			if (beanType.getName().equals(type)) {
				return true;
			}
			// Attempt to load the type
			try {
				Class<?> actualType = ClassUtils.forName(type, this.classloader);
				return beanType.isAssignableFrom(actualType);
			}
			catch (Throwable ex) {
				// Class not available, ignore
			}
			return false;
		}

		private static boolean isPublic(MethodMetadata methodMetadata) {
			int access = (Integer) new DirectFieldAccessor(methodMetadata)
					.getPropertyValue("access");
			return (access & Opcodes.ACC_PUBLIC) != 0;
		}
	}

	private final class NoMatchDescriptionProvider {

		private final MethodMetadata methodMetadata;

		private final ConditionOutcome conditionOutcome;

		private final boolean methodEvaluated;


		private NoMatchDescriptionProvider(MethodMetadata methodMetadata,
				ConditionOutcome conditionOutcome, boolean methodEvaluated) {
			this.methodMetadata = methodMetadata;
			this.conditionOutcome = conditionOutcome;
			this.methodEvaluated = methodEvaluated;
		}


		public String generateNoMatchDescription() {
			String message = this.conditionOutcome.getMessage();
			message += generateDescriptionDetail();
			return message;
		}

		private String generateDescriptionDetail() {
			if (this.methodEvaluated) {
				return String.format(": disabled on method '%s' in '%s'",
						this.methodMetadata.getMethodName(), this.methodMetadata.getDeclaringClassName());
			}
			else {
				return String.format(": method '%s' was a candidate", this.methodMetadata.getMethodName());
			}
		}

		public String toString() {
			return generateNoMatchDescription();
		}
	}

}
