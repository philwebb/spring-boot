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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.dynamic.support.ResolvableType;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;

/**
 * A base {@link EndpointDiscoverer} implementation that discovers
 * {@link Endpoint @Endpoint} beans and {@link EndpointExtension @EndpointExtension} beans
 * in an application context.
 *
 * @param <K> the type of the operation key
 * @param <T> the type of the operation
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AnnotationEndpointDiscoverer<K, T extends Operation>
		implements EndpointDiscoverer<T> {

	private final ApplicationContext applicationContext;

	private final Function<T, K> operationKeyFactory;

	private final AnnotatedEndpointOperationsFactory<T> operationsFactory;

	protected AnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			AnnotatedEndpointOperationFactory<T> operationFactory,
			Function<T, K> operationKeyFactory,
			CachingConfigurationFactory cachingConfigurationFactory) {
		this.applicationContext = applicationContext;
		this.operationKeyFactory = operationKeyFactory;
		this.operationsFactory = new AnnotatedEndpointOperationsFactory<>(
				operationFactory, cachingConfigurationFactory);
	}

	@Override
	public final Collection<EndpointInfo<T>> discoverEndpoints() {
		Collection<EndpointInfoDescriptor<T, K>> descriptors = getDescriptors();
		verify(descriptors);
		return descriptors.stream().map(EndpointInfoDescriptor::getEndpointInfo)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private Collection<EndpointInfoDescriptor<T, K>> getDescriptors() {
		Class<T> operationType = getOperationType();
		Map<Class<?>, EndpointInfo<T>> endpoints = getEndpoints(operationType);
		Map<Class<?>, EndpointExtensionInfo<T>> extensions = getExtensions(operationType,
				endpoints);
		return getDescriptors(endpoints, extensions);
	}

	/**
	 * Return the operation type being discovered. By default this method will resolve the
	 * class generic "{@code <T>}".
	 * @return the operation type
	 */
	@SuppressWarnings("unchecked")
	protected Class<T> getOperationType() {
		return (Class<T>) ResolvableType
				.forClass(getClass(), AnnotationEndpointDiscoverer.class)
				.resolveGeneric(1);
	}

	private Map<Class<?>, EndpointInfo<T>> getEndpoints(Class<T> operationType) {
		Map<Class<?>, EndpointInfo<T>> endpoints = new LinkedHashMap<>();
		Map<String, EndpointInfo<T>> endpointsById = new LinkedHashMap<>();
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, Endpoint.class);
		for (String beanName : beanNames) {
			addEndpoint(endpoints, endpointsById, beanName);
		}
		return endpoints;
	}

	private void addEndpoint(Map<Class<?>, EndpointInfo<T>> endpoints,
			Map<String, EndpointInfo<T>> endpointsById, String beanName) {
		Class<?> endpointType = this.applicationContext.getType(beanName);
		Object target = this.applicationContext.getBean(beanName);
		EndpointInfo<T> endpoint = createEndpoint(target, endpointType);
		String id = endpoint.getId();
		if (isEndpointExposed(endpointType, endpoint)) {
			EndpointInfo<T> previous = endpointsById.putIfAbsent(id, endpoint);
			Assert.state(previous == null, () -> "Found two endpoints with the id '" + id
					+ "': " + endpoint + " and " + previous);
			endpoints.put(endpointType, endpoint);
		}
	}

	private EndpointInfo<T> createEndpoint(Object target, Class<?> endpointType) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.findMergedAnnotationAttributes(endpointType, Endpoint.class, true, true);
		String id = annotationAttributes.getString("id");
		boolean enabledByDefault = (Boolean) annotationAttributes.get("enableByDefault");
		Collection<T> operations = this.operationsFactory
				.createOperations(id, target, endpointType).values();
		return new EndpointInfo<>(id, enabledByDefault, operations);
	}

	private Map<Class<?>, EndpointExtensionInfo<T>> getExtensions(Class<T> operationType,
			Map<Class<?>, EndpointInfo<T>> endpoints) {
		Map<Class<?>, EndpointExtensionInfo<T>> extensions = new LinkedHashMap<>();
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, EndpointExtension.class);
		for (String beanName : beanNames) {
			addExtension(endpoints, extensions, beanName);
		}
		return extensions;
	}

	private void addExtension(Map<Class<?>, EndpointInfo<T>> endpoints,
			Map<Class<?>, EndpointExtensionInfo<T>> extensions, String beanName) {
		Class<?> extensionType = this.applicationContext.getType(beanName);
		Class<?> endpointType = getEndpointType(extensionType);
		EndpointInfo<T> endpoint = getEndpoint(endpoints, extensionType, endpointType);
		Assert.state(isEndpointExposed(endpointType, endpoint),
				() -> "Invalid extension " + extensionType.getName() + "': endpoint '"
						+ endpointType.getName() + "' does not support such extension");
		if (isExtensionExposed(extensionType, endpoint)) {
			Object target = this.applicationContext.getBean(beanName);
			Map<Method, T> operations = this.operationsFactory
					.createOperations(endpoint.getId(), target, extensionType);
			EndpointExtensionInfo<T> extension = new EndpointExtensionInfo<>(
					extensionType, operations.values());
			EndpointExtensionInfo<T> previous = extensions.putIfAbsent(endpointType,
					extension);
			Assert.state(previous == null,
					() -> "Found two extensions for the same endpoint '"
							+ endpointType.getName() + "': "
							+ extension.getExtensionType().getName() + " and "
							+ previous.getExtensionType().getName());
		}
	}

	private Class<?> getEndpointType(Class<?> extensionType) {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(extensionType, EndpointExtension.class);
		Class<?> endpointType = attributes.getClass("endpoint");
		Assert.state(!endpointType.equals(Void.class), () -> "Extension "
				+ endpointType.getName() + " does not specify an endpoint");
		return endpointType;
	}

	private EndpointInfo<T> getEndpoint(Map<Class<?>, EndpointInfo<T>> endpoints,
			Class<?> extensionType, Class<?> endpointType) {
		EndpointInfo<T> endpoint = endpoints.get(endpointType);
		Assert.state(endpoint != null,
				() -> "Invalid extension '" + extensionType.getName()
						+ "': no endpoint found with type '" + endpointType.getName()
						+ "'");
		return endpoint;
	}

	private boolean isEndpointExposed(Class<?> endpointType, EndpointInfo<T> endpoint) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(endpointType, FilteredEndpoint.class);
		if (annotationAttributes == null) {
			return true;
		}
		Class<?> filterClass = annotationAttributes.getClass("value");
		return !isFiltered(filterClass, endpoint);
	}

	private boolean isExtensionExposed(Class<?> extensionType, EndpointInfo<T> endpoint) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(extensionType, EndpointExtension.class);
		Class<?> filterClass = annotationAttributes.getClass("filter");
		return !isFiltered(filterClass, endpoint);
	}

	@SuppressWarnings("unchecked")
	private boolean isFiltered(Class<?> filterClass, EndpointInfo<T> endpoint) {
		Class<?> generic = ResolvableType.forClass(filterClass, EndpointFilter.class)
				.resolveGeneric(0);
		if (!generic.isAssignableFrom(filterClass)) {
			return true;
		}
		EndpointFilter<T> filter = (EndpointFilter<T>) BeanUtils
				.instantiateClass(filterClass);
		return filter.match(endpoint, this);
	}

	private Collection<EndpointInfoDescriptor<T, K>> getDescriptors(
			Map<Class<?>, EndpointInfo<T>> endpoints,
			Map<Class<?>, EndpointExtensionInfo<T>> extensions) {
		List<EndpointInfoDescriptor<T, K>> result = new ArrayList<>();
		endpoints.forEach((endpointClass, endpointInfo) -> {
			EndpointExtensionInfo<T> extension = extensions.remove(endpointClass);
			result.add(createDescriptor(endpointClass, endpointInfo, extension));
		});
		return result;
	}

	private EndpointInfoDescriptor<T, K> createDescriptor(Class<?> type,
			EndpointInfo<T> endpoint, EndpointExtensionInfo<T> extension) {
		String endpointId = endpoint.getId();
		Map<OperationKey<K>, List<T>> operations = indexOperations(endpointId, type,
				endpoint.getOperations());
		if (extension != null) {
			Map<OperationKey<K>, List<T>> extensionOperations = indexOperations(
					endpointId, extension.getExtensionType(), extension.getOperations());
			operations.putAll(extensionOperations);
			return new EndpointInfoDescriptor<>(mergeExtension(endpoint, extension),
					operations);
		}
		return new EndpointInfoDescriptor<>(endpoint, operations);
	}

	private Map<OperationKey<K>, List<T>> indexOperations(String endpointId,
			Class<?> target, Collection<T> operations) {
		LinkedMultiValueMap<OperationKey<K>, T> result = new LinkedMultiValueMap<>();
		operations.forEach((operation) -> {
			K key = this.operationKeyFactory.apply(operation);
			result.add(new OperationKey<>(endpointId, target, key), operation);
		});
		return result;
	}

	private EndpointInfo<T> mergeExtension(EndpointInfo<T> endpoint,
			EndpointExtensionInfo<T> extension) {
		Map<K, T> operations = new HashMap<>();
		Consumer<T> consumer = (operation) -> operations
				.put(this.operationKeyFactory.apply(operation), operation);
		endpoint.getOperations().forEach(consumer);
		extension.getOperations().forEach(consumer);
		return new EndpointInfo<>(endpoint.getId(), endpoint.isEnableByDefault(),
				operations.values());
	}

	/**
	 * Allows subclasses to verify that the descriptors are correctly configured.
	 * @param descriptors the descriptors to verify
	 */
	protected void verify(Collection<EndpointInfoDescriptor<T, K>> descriptors) {
	}

	/**
	 * Describes an {@link EndpointInfo endpoint} and whether or not it is valid.
	 *
	 * @param <T> the type of the operation
	 * @param <K> the type of the operation key
	 */
	protected static final class EndpointInfoDescriptor<T extends Operation, K> {

		private final EndpointInfo<T> endpointInfo;

		private final Map<OperationKey<K>, List<T>> operations;

		protected EndpointInfoDescriptor(EndpointInfo<T> endpointInfo,
				Map<OperationKey<K>, List<T>> operations) {
			this.endpointInfo = endpointInfo;
			this.operations = operations;
		}

		public EndpointInfo<T> getEndpointInfo() {
			return this.endpointInfo;
		}

		public Map<OperationKey<K>, List<T>> findDuplicateOperations() {
			Map<OperationKey<K>, List<T>> duplicateOperations = new HashMap<>();
			this.operations.forEach((k, list) -> {
				if (list.size() > 1) {
					duplicateOperations.put(k, list);
				}
			});
			return duplicateOperations;
		}

	}

	/**
	 * Describes a tech-specific extension of an endpoint.
	 *
	 * @param <T> the type of the operation
	 */
	protected static final class EndpointExtensionInfo<T extends Operation> {

		private final Class<?> extensionType;

		private final Collection<T> operations;

		private EndpointExtensionInfo(Class<?> extensionType, Collection<T> operations) {
			this.extensionType = extensionType;
			this.operations = operations;
		}

		public Class<?> getExtensionType() {
			return this.extensionType;
		}

		public Collection<T> getOperations() {
			return this.operations;
		}

	}

	/**
	 * Define the key of an operation in the context of an operation's implementation.
	 *
	 * @param <K> the type of the key
	 */
	protected static final class OperationKey<K> {

		private final String endpointId;

		private final Class<?> endpointType;

		private final K key;

		public OperationKey(String endpointId, Class<?> endpointType, K key) {
			this.endpointId = endpointId;
			this.endpointType = endpointType;
			this.key = key;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			OperationKey<?> other = (OperationKey<?>) o;
			Boolean result = true;
			result = result && this.endpointId.equals(other.endpointId);
			result = result && this.endpointType.equals(other.endpointType);
			result = result && this.key.equals(other.key);
			return result;
		}

		@Override
		public int hashCode() {
			int result = this.endpointId.hashCode();
			result = 31 * result + this.endpointType.hashCode();
			result = 31 * result + this.key.hashCode();
			return result;
		}

	}

}
