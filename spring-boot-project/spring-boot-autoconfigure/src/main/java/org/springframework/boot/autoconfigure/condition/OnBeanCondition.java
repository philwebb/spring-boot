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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotation.Adapt;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific beans.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Jakub Kubrynski
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @see ConditionalOnBean
 * @see ConditionalOnMissingBean
 * @see ConditionalOnSingleCandidate
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnBeanCondition extends FilteringSpringBootCondition implements ConfigurationCondition {

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	protected final ConditionOutcome[] getOutcomes(String[] autoConfigurationClasses,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
		for (int i = 0; i < outcomes.length; i++) {
			String autoConfigurationClass = autoConfigurationClasses[i];
			if (autoConfigurationClass != null) {
				Set<String> onBeanTypes = autoConfigurationMetadata.getSet(autoConfigurationClass, "ConditionalOnBean");
				outcomes[i] = getOutcome(onBeanTypes, ConditionalOnBean.class);
				if (outcomes[i] == null) {
					Set<String> onSingleCandidateTypes = autoConfigurationMetadata.getSet(autoConfigurationClass,
							"ConditionalOnSingleCandidate");
					outcomes[i] = getOutcome(onSingleCandidateTypes, ConditionalOnSingleCandidate.class);
				}
			}
		}
		return outcomes;
	}

	private ConditionOutcome getOutcome(Set<String> requiredBeanTypes, Class<? extends Annotation> annotation) {
		List<String> missing = filter(requiredBeanTypes, ClassNameFilter.MISSING, getBeanClassLoader());
		if (!missing.isEmpty()) {
			ConditionMessage message = ConditionMessage.forCondition(annotation)
					.didNotFind("required type", "required types").items(Style.QUOTE, missing);
			return ConditionOutcome.noMatch(message);
		}
		return null;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		MergedAnnotations annotations = metadata.getAnnotations();
		if (annotations.isPresent(ConditionalOnBean.class)) {
			Spec<ConditionalOnBean> spec = new Spec<>(context, metadata, annotations, ConditionalOnBean.class);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (!matchResult.isAllMatched()) {
				String reason = createOnBeanNoMatchReason(matchResult);
				return ConditionOutcome.noMatch(spec.message().because(reason));
			}
			matchMessage = spec.message(matchMessage).found("bean", "beans").items(Style.QUOTE,
					matchResult.getNamesOfAllMatches());
		}
		if (metadata.isAnnotated(ConditionalOnSingleCandidate.class.getName())) {
			Spec<ConditionalOnSingleCandidate> spec = new SingleCandidateSpec(context, metadata, annotations);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (!matchResult.isAllMatched()) {
				return ConditionOutcome.noMatch(spec.message().didNotFind("any beans").atAll());
			}
			else if (!hasSingleAutowireCandidate(context.getBeanFactory(), matchResult.getNamesOfAllMatches(),
					spec.getStrategy() == SearchStrategy.ALL)) {
				return ConditionOutcome.noMatch(spec.message().didNotFind("a primary bean from beans")
						.items(Style.QUOTE, matchResult.getNamesOfAllMatches()));
			}
			matchMessage = spec.message(matchMessage).found("a primary bean from beans").items(Style.QUOTE,
					matchResult.getNamesOfAllMatches());
		}
		if (metadata.isAnnotated(ConditionalOnMissingBean.class.getName())) {
			Spec<ConditionalOnMissingBean> spec = new Spec<>(context, metadata, annotations,
					ConditionalOnMissingBean.class);
			MatchResult matchResult = getMatchingBeans(context, spec);
			if (matchResult.isAnyMatched()) {
				String reason = createOnMissingBeanNoMatchReason(matchResult);
				return ConditionOutcome.noMatch(spec.message().because(reason));
			}
			matchMessage = spec.message(matchMessage).didNotFind("any beans").atAll();
		}
		return ConditionOutcome.match(matchMessage);
	}

	protected final MatchResult getMatchingBeans(ConditionContext context, Spec<?> spec) {
		ClassLoader classLoader = context.getClassLoader();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		boolean considerHierarchy = spec.getStrategy() != SearchStrategy.CURRENT;
		String[] parameterizedContainers = spec.getParameterizedContainers();
		if (spec.getStrategy() == SearchStrategy.ANCESTORS) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.isInstanceOf(ConfigurableListableBeanFactory.class, parent, "Unable to use SearchStrategy.PARENTS");
			beanFactory = (ConfigurableListableBeanFactory) parent;
		}
		MatchResult result = new MatchResult();
		Set<String> beansIgnoredByType = getNamesOfBeansIgnoredByType(classLoader, beanFactory, considerHierarchy,
				spec.getIgnoredTypes(), parameterizedContainers);
		for (String type : spec.getTypes()) {
			Collection<String> typeMatches = getBeanNamesForType(classLoader, considerHierarchy, beanFactory, type,
					parameterizedContainers);
			typeMatches.removeAll(beansIgnoredByType);
			if (typeMatches.isEmpty()) {
				result.recordUnmatchedType(type);
			}
			else {
				result.recordMatchedType(type, typeMatches);
			}
		}
		for (String annotation : spec.getAnnotations()) {
			Set<String> annotationMatches = getBeanNamesForAnnotation(classLoader, beanFactory, annotation,
					considerHierarchy);
			annotationMatches.removeAll(beansIgnoredByType);
			if (annotationMatches.isEmpty()) {
				result.recordUnmatchedAnnotation(annotation);
			}
			else {
				result.recordMatchedAnnotation(annotation, annotationMatches);
			}
		}
		for (String beanName : spec.getNames()) {
			if (!beansIgnoredByType.contains(beanName) && containsBean(beanFactory, beanName, considerHierarchy)) {
				result.recordMatchedName(beanName);
			}
			else {
				result.recordUnmatchedName(beanName);
			}
		}
		return result;
	}

	private Set<String> getNamesOfBeansIgnoredByType(ClassLoader classLoader, ListableBeanFactory beanFactory,
			boolean considerHierarchy, String[] ignoredTypes, String[] parameterizedContainers) {
		Set<String> result = null;
		for (String ignoredType : ignoredTypes) {
			Collection<String> ignoredNames = getBeanNamesForType(classLoader, considerHierarchy, beanFactory,
					ignoredType, parameterizedContainers);
			result = addAll(result, ignoredNames);
		}
		return (result != null) ? result : Collections.emptySet();
	}

	private Set<String> getBeanNamesForType(ClassLoader classLoader, boolean considerHierarchy,
			ListableBeanFactory beanFactory, String type, String[] parameterizedContainers) throws LinkageError {
		try {
			return getBeanNamesForType(beanFactory, considerHierarchy, ClassUtils.forName(type, classLoader),
					resolveParameterizedContainers(parameterizedContainers, classLoader));
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			return Collections.emptySet();
		}
	}

	private Class<?>[] resolveParameterizedContainers(String[] parameterizedContainers, ClassLoader classLoader) {
		Class<?>[] resolved = new Class<?>[parameterizedContainers.length];
		for (int i = 0; i < parameterizedContainers.length; i++) {
			try {
				resolved[i] = ClassUtils.forName(parameterizedContainers[i], classLoader);
			}
			catch (ClassNotFoundException | NoClassDefFoundError ex) {
			}
		}
		return resolved;
	}

	private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type,
			Class<?>[] parameterizedContainers) {
		Set<String> result = getBeanNamesForType(beanFactory, considerHierarchy, type, parameterizedContainers, null);
		return (result != null) ? result : Collections.emptySet();
	}

	private Set<String> getBeanNamesForType(ListableBeanFactory beanFactory, boolean considerHierarchy, Class<?> type,
			Class<?>[] parameterizedContainers, Set<String> result) {
		String[] names = beanFactory.getBeanNamesForType(type, true, false);
		if (names.length > 0) {
			result = addAll(result, names);
		}
		for (int i = 0; i < parameterizedContainers.length; i++) {
			if (parameterizedContainers[i] != null) {
				// FIXME
			}
		}
		BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
		if (considerHierarchy && parent instanceof ListableBeanFactory) {
			result = getBeanNamesForType((ListableBeanFactory) parent, considerHierarchy, type, parameterizedContainers,
					result);
		}
		return result;
	}

	private Set<String> getBeanNamesForAnnotation(ClassLoader classLoader, ConfigurableListableBeanFactory beanFactory,
			String type, boolean considerHierarchy) throws LinkageError {
		Set<String> result = null;
		try {
			result = getBeanNamesForAnnotation(beanFactory, resolveAnnotationType(classLoader, type), considerHierarchy,
					result);
		}
		catch (ClassNotFoundException ex) {
			// Continue
		}
		return (result != null) ? result : Collections.emptySet();
	}

	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> resolveAnnotationType(ClassLoader classLoader, String type)
			throws ClassNotFoundException {
		return (Class<? extends Annotation>) ClassUtils.forName(type, classLoader);
	}

	private Set<String> getBeanNamesForAnnotation(ListableBeanFactory beanFactory,
			Class<? extends Annotation> annotationType, boolean considerHierarchy, Set<String> result) {
		result = addAll(result, beanFactory.getBeanNamesForAnnotation(annotationType));
		if (considerHierarchy) {
			BeanFactory parent = ((HierarchicalBeanFactory) beanFactory).getParentBeanFactory();
			if (parent instanceof ListableBeanFactory) {
				result = getBeanNamesForAnnotation((ListableBeanFactory) parent, annotationType, considerHierarchy,
						result);
			}
		}
		return result;
	}

	private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName,
			boolean considerHierarchy) {
		if (considerHierarchy) {
			return beanFactory.containsBean(beanName);
		}
		return beanFactory.containsLocalBean(beanName);
	}

	private Set<String> addAll(Set<String> result, String[] additional) {
		if (ObjectUtils.isEmpty(additional)) {
			return result;
		}
		result = (result != null) ? result : new LinkedHashSet<>();
		for (String name : additional) {
			result.add(name);
		}
		return result;
	}

	private Set<String> addAll(Set<String> result, Collection<String> additional) {
		if (CollectionUtils.isEmpty(additional)) {
			return result;
		}
		result = (result != null) ? result : new LinkedHashSet<>();
		result.addAll(additional);
		return result;
	}

	private String createOnBeanNoMatchReason(MatchResult matchResult) {
		StringBuilder reason = new StringBuilder();
		appendMessageForNoMatches(reason, matchResult.getUnmatchedAnnotations(), "annotated with");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedTypes(), "of type");
		appendMessageForNoMatches(reason, matchResult.getUnmatchedNames(), "named");
		return reason.toString();
	}

	private void appendMessageForNoMatches(StringBuilder reason, Collection<String> unmatched, String description) {
		if (!unmatched.isEmpty()) {
			if (reason.length() > 0) {
				reason.append(" and ");
			}
			reason.append("did not find any beans ");
			reason.append(description);
			reason.append(" ");
			reason.append(StringUtils.collectionToDelimitedString(unmatched, ", "));
		}
	}

	private String createOnMissingBeanNoMatchReason(MatchResult matchResult) {
		StringBuilder reason = new StringBuilder();
		appendMessageForMatches(reason, matchResult.getMatchedAnnotations(), "annotated with");
		appendMessageForMatches(reason, matchResult.getMatchedTypes(), "of type");
		if (!matchResult.getMatchedNames().isEmpty()) {
			if (reason.length() > 0) {
				reason.append(" and ");
			}
			reason.append("found beans named ");
			reason.append(StringUtils.collectionToDelimitedString(matchResult.getMatchedNames(), ", "));
		}
		return reason.toString();
	}

	private void appendMessageForMatches(StringBuilder reason, Map<String, Collection<String>> matches,
			String description) {
		if (!matches.isEmpty()) {
			matches.forEach((key, value) -> {
				if (reason.length() > 0) {
					reason.append(" and ");
				}
				reason.append("found beans ");
				reason.append(description);
				reason.append(" '");
				reason.append(key);
				reason.append("' ");
				reason.append(StringUtils.collectionToDelimitedString(value, ", "));
			});
		}
	}

	private boolean hasSingleAutowireCandidate(ConfigurableListableBeanFactory beanFactory, Set<String> beanNames,
			boolean considerHierarchy) {
		return (beanNames.size() == 1 || getPrimaryBeans(beanFactory, beanNames, considerHierarchy).size() == 1);
	}

	private List<String> getPrimaryBeans(ConfigurableListableBeanFactory beanFactory, Set<String> beanNames,
			boolean considerHierarchy) {
		List<String> primaryBeans = new ArrayList<>();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = findBeanDefinition(beanFactory, beanName, considerHierarchy);
			if (beanDefinition != null && beanDefinition.isPrimary()) {
				primaryBeans.add(beanName);
			}
		}
		return primaryBeans;
	}

	private BeanDefinition findBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName,
			boolean considerHierarchy) {
		if (beanFactory.containsBeanDefinition(beanName)) {
			return beanFactory.getBeanDefinition(beanName);
		}
		if (considerHierarchy && beanFactory.getParentBeanFactory() instanceof ConfigurableListableBeanFactory) {
			return findBeanDefinition(((ConfigurableListableBeanFactory) beanFactory.getParentBeanFactory()), beanName,
					considerHierarchy);
		}
		return null;
	}

	/**
	 * A search specification extracted from the underlying annotation.
	 */
	private static class Spec<A extends Annotation> {

		private static final String[] EMPTY = {};

		private final Class<?> annotationType;

		private final String[] names;

		private final String[] types;

		private final String[] annotations;

		private final String[] ignoredTypes;

		private final String[] parameterizedContainers;

		private final SearchStrategy strategy;

		Spec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations,
				Class<A> annotationType) {
			MergedAnnotation<A> annotation = annotations.get(annotationType);
			Map<String, Object> attributes = annotation.asMap(Adapt.CLASS_TO_STRING);
			this.annotationType = annotationType;
			this.names = extract(attributes, "name");
			this.annotations = extract(attributes, "annotation");
			this.ignoredTypes = extract(attributes, "ignored", "ignoredType");
			this.parameterizedContainers = extract(attributes, "parameterizedContainer");
			this.strategy = annotation.getValue("search", SearchStrategy.class).orElse(null);
			String[] types = extractTypes(attributes);
			BeanTypeDeductionException deductionException = null;
			if (ObjectUtils.isEmpty(types) && ObjectUtils.isEmpty(this.names)) {
				try {
					types = deducedBeanType(context, metadata);
				}
				catch (BeanTypeDeductionException ex) {
					deductionException = ex;
				}
			}
			this.types = types;
			validate(deductionException);
		}

		protected String[] extractTypes(Map<String, Object> attributes) {
			return extract(attributes, "value", "type");
		}

		private String[] extract(Map<String, Object> attributes, String... attributeNames) {
			String[] result = EMPTY;
			for (String attributeName : attributeNames) {
				Object value = attributes.get(attributeName);
				if (value instanceof String[]) {
					result = merge(result, (String[]) value);
				}
				else if (value instanceof String) {
					result = merge(result, (String) value);
				}
			}
			return result;
		}

		private String[] merge(String[] source, String... additional) {
			if (source.length == 0) {
				return additional;
			}
			if (additional.length == 0) {
				return source;
			}
			String[] result = new String[source.length + additional.length];
			System.arraycopy(source, 0, result, 0, source.length);
			System.arraycopy(additional, 0, result, additional.length, additional.length);
			return result;
		}

		protected void validate(BeanTypeDeductionException ex) {
			if (!hasAtLeastOne(this.types, this.names, this.annotations)) {
				String message = getAnnotationName() + " did not specify a bean using type, name or annotation";
				if (ex == null) {
					throw new IllegalStateException(message);
				}
				throw new IllegalStateException(message + " and the attempt to deduce the bean's type failed", ex);
			}
		}

		private boolean hasAtLeastOne(String[]... arrays) {
			for (String[] array : arrays) {
				if (!ObjectUtils.isEmpty(array)) {
					return true;
				}
			}
			return false;
		}

		protected final String getAnnotationName() {
			return "@" + ClassUtils.getShortName(this.annotationType);
		}

		private String[] deducedBeanType(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (metadata instanceof MethodMetadata && metadata.isAnnotated(Bean.class.getName())) {
				return deducedBeanTypeForBeanMethod(context, (MethodMetadata) metadata);
			}
			return EMPTY;
		}

		private String[] deducedBeanTypeForBeanMethod(ConditionContext context, MethodMetadata metadata) {
			try {
				Class<?> returnType = getReturnType(context, metadata);
				return new String[] { returnType.getName() };
			}
			catch (Throwable ex) {
				throw new BeanTypeDeductionException(metadata.getDeclaringClassName(), metadata.getMethodName(), ex);
			}
		}

		private Class<?> getReturnType(ConditionContext context, MethodMetadata metadata)
				throws ClassNotFoundException, LinkageError {
			// Safe to load at this point since we are in the REGISTER_BEAN phase
			ClassLoader classLoader = context.getClassLoader();
			Class<?> returnType = ClassUtils.forName(metadata.getReturnTypeName(), classLoader);
			if (isParameterizedContainer(returnType, classLoader)) {
				returnType = getReturnTypeGeneric(metadata, classLoader);
			}
			return returnType;
		}

		private Class<?> getReturnTypeGeneric(MethodMetadata metadata, ClassLoader classLoader)
				throws ClassNotFoundException, LinkageError {
			Class<?> declaringClass = ClassUtils.forName(metadata.getDeclaringClassName(), classLoader);
			Method beanMethod = findBeanMethod(declaringClass, metadata.getMethodName());
			return ResolvableType.forMethodReturnType(beanMethod).resolveGeneric();
		}

		private Method findBeanMethod(Class<?> declaringClass, String methodName) {
			Method method = ReflectionUtils.findMethod(declaringClass, methodName);
			if (isBeanMethod(method)) {
				return method;
			}
			Method[] candidates = ReflectionUtils.getAllDeclaredMethods(declaringClass);
			for (Method candidate : candidates) {
				if (candidate.getName().equals(methodName) && isBeanMethod(candidate)) {
					return candidate;
				}
			}
			throw new IllegalStateException("Unable to find bean method " + methodName);
		}

		private boolean isBeanMethod(Method method) {
			return method != null && MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.EXHAUSTIVE)
					.isPresent(Bean.class);
		}

		private boolean isParameterizedContainer(Class<?> type, ClassLoader classLoader) {
			for (String candidate : this.parameterizedContainers) {
				try {
					if (ClassUtils.forName(candidate, classLoader).isAssignableFrom(type)) {
						return true;
					}
				}
				catch (Exception ex) {
				}
			}
			return false;
		}

		private SearchStrategy getStrategy() {
			return (this.strategy != null) ? this.strategy : SearchStrategy.ALL;
		}

		String[] getNames() {
			return this.names;
		}

		final String[] getTypes() {
			return this.types;
		}

		String[] getAnnotations() {
			return this.annotations;
		}

		String[] getIgnoredTypes() {
			return this.ignoredTypes;
		}

		String[] getParameterizedContainers() {
			return this.parameterizedContainers;
		}

		ConditionMessage.Builder message() {
			return ConditionMessage.forCondition(ConditionalOnBean.class, this);
		}

		ConditionMessage.Builder message(ConditionMessage message) {
			return message.andCondition(ConditionalOnBean.class, this);
		}

		@Override
		public String toString() {
			boolean hasNames = ObjectUtils.isEmpty(this.names);
			boolean hasTypes = ObjectUtils.isEmpty(this.types);
			StringBuilder string = new StringBuilder();
			string.append("(");
			if (!hasNames) {
				string.append("names: ");
				string.append(StringUtils.arrayToCommaDelimitedString(this.names));
				string.append(hasTypes ? "" : "; ");
			}
			if (!hasTypes) {
				string.append("types: ");
				string.append(StringUtils.arrayToCommaDelimitedString(this.types));
			}
			string.append("; SearchStrategy: ");
			string.append(this.strategy.toString().toLowerCase(Locale.ENGLISH));
			string.append(")");
			return string.toString();
		}

	}

	/**
	 * Specialized {@link Spec specification} for
	 * {@link ConditionalOnSingleCandidate @ConditionalOnSingleCandidate}.
	 */
	private static class SingleCandidateSpec extends Spec<ConditionalOnSingleCandidate> {

		private static final String[] FILTERED_TYPES = { "", Object.class.getName() };

		SingleCandidateSpec(ConditionContext context, AnnotatedTypeMetadata metadata, MergedAnnotations annotations) {
			super(context, metadata, annotations, ConditionalOnSingleCandidate.class);
		}

		@Override
		protected String[] extractTypes(Map<String, Object> attributes) {
			String[] types = super.extractTypes(attributes);
			String[] filteredTypes = null;
			int size = 0;
			for (int i = 0; i < types.length; i++) {
				if (isFilteredType(types[i])) {
					if (filteredTypes == null) {
						filteredTypes = new String[types.length];
						System.arraycopy(types, 0, filteredTypes, 0, i);
					}
				}
				else {
					if (filteredTypes != null) {
						filteredTypes[size] = types[i];
					}
					size++;
				}
			}
			if (filteredTypes != null) {
				String[] result = new String[size];
				System.arraycopy(filteredTypes, 0, result, 0, size);
				return result;
			}
			return types;
		}

		private boolean isFilteredType(String type) {
			for (String filteredType : FILTERED_TYPES) {
				if (type.equals(filteredType)) {
					return true;
				}
			}
			return false;
		}

		@Override
		protected void validate(BeanTypeDeductionException ex) {
			Assert.isTrue(getTypes().length == 1,
					() -> getAnnotationName() + " annotations must specify only one type (got "
							+ StringUtils.arrayToCommaDelimitedString(getTypes()) + ")");
		}

	}

	/**
	 * Results collected during the condition evaluation.
	 */
	private static final class MatchResult {

		private final Map<String, Collection<String>> matchedAnnotations = new HashMap<>();

		private final List<String> matchedNames = new ArrayList<>();

		private final Map<String, Collection<String>> matchedTypes = new HashMap<>();

		private final List<String> unmatchedAnnotations = new ArrayList<>();

		private final List<String> unmatchedNames = new ArrayList<>();

		private final List<String> unmatchedTypes = new ArrayList<>();

		private final Set<String> namesOfAllMatches = new HashSet<>();

		private void recordMatchedName(String name) {
			this.matchedNames.add(name);
			this.namesOfAllMatches.add(name);
		}

		private void recordUnmatchedName(String name) {
			this.unmatchedNames.add(name);
		}

		private void recordMatchedAnnotation(String annotation, Collection<String> matchingNames) {
			this.matchedAnnotations.put(annotation, matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		private void recordUnmatchedAnnotation(String annotation) {
			this.unmatchedAnnotations.add(annotation);
		}

		private void recordMatchedType(String type, Collection<String> matchingNames) {
			this.matchedTypes.put(type, matchingNames);
			this.namesOfAllMatches.addAll(matchingNames);
		}

		private void recordUnmatchedType(String type) {
			this.unmatchedTypes.add(type);
		}

		public boolean isAllMatched() {
			return this.unmatchedAnnotations.isEmpty() && this.unmatchedNames.isEmpty()
					&& this.unmatchedTypes.isEmpty();
		}

		public boolean isAnyMatched() {
			return (!this.matchedAnnotations.isEmpty()) || (!this.matchedNames.isEmpty())
					|| (!this.matchedTypes.isEmpty());
		}

		public Map<String, Collection<String>> getMatchedAnnotations() {
			return this.matchedAnnotations;
		}

		public List<String> getMatchedNames() {
			return this.matchedNames;
		}

		public Map<String, Collection<String>> getMatchedTypes() {
			return this.matchedTypes;
		}

		public List<String> getUnmatchedAnnotations() {
			return this.unmatchedAnnotations;
		}

		public List<String> getUnmatchedNames() {
			return this.unmatchedNames;
		}

		public List<String> getUnmatchedTypes() {
			return this.unmatchedTypes;
		}

		public Set<String> getNamesOfAllMatches() {
			return this.namesOfAllMatches;
		}

	}

	/**
	 * Exteption thrown when the bean type cannot be deduced.
	 */
	static class BeanTypeDeductionException extends RuntimeException {

		private BeanTypeDeductionException(String className, String beanMethodName, Throwable cause) {
			super("Failed to deduce bean type for " + className + "." + beanMethodName, cause);
		}

	}

}
