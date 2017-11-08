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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Factory to creates an {@link Operation} for a annotated methods on an
 * {@link Endpoint @Endpoint}.
 *
 * @param <T> The operation type
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class AnnotatedEndpointOperationsFactory<T extends Operation> {

	private static final Map<OperationType, Class<? extends Annotation>> OPERATION_TYPES;

	static {
		Map<OperationType, Class<? extends Annotation>> operationTypes = new LinkedHashMap<>();
		operationTypes.put(OperationType.READ, ReadOperation.class);
		operationTypes.put(OperationType.WRITE, WriteOperation.class);
		operationTypes.put(OperationType.DELETE, DeleteOperation.class);
		OPERATION_TYPES = Collections.unmodifiableMap(operationTypes);
	}

	private final AnnotatedEndpointOperationFactory<T> operationFactory;

	private final CachingConfigurationFactory cachingConfigurationFactory;

	AnnotatedEndpointOperationsFactory(
			AnnotatedEndpointOperationFactory<T> operationFactory,
			CachingConfigurationFactory cachingConfigurationFactory) {
		super();
		this.operationFactory = operationFactory;
		this.cachingConfigurationFactory = cachingConfigurationFactory;
	}

	public Map<Method, T> createOperations(String id, Object target, Class<?> type) {
		return MethodIntrospector.selectMethods(type,
				(MetadataLookup<T>) (method) -> createOperation(id, target, method));
	}

	private T createOperation(String endpointId, Object target, Method method) {
		return OPERATION_TYPES.entrySet().stream()
				.map((entry) -> createOperation(endpointId, target, method,
						entry.getKey(), entry.getValue()))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	private T createOperation(String endpointId, Object target, Method method,
			OperationType type, Class<? extends Annotation> annotationType) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, annotationType);
		if (annotationAttributes == null) {
			return null;
		}
		CachingConfiguration cachingConfiguration = this.cachingConfigurationFactory
				.getCachingConfiguration(endpointId);
		long timeToLive = determineTimeToLive(cachingConfiguration, type, method);
		return this.operationFactory.createOperation(endpointId, annotationAttributes,
				target, method, type, timeToLive);
	}

	private long determineTimeToLive(CachingConfiguration cachingConfiguration,
			OperationType operationType, Method method) {
		if (cachingConfiguration != null && cachingConfiguration.getTimeToLive() > 0
				&& operationType == OperationType.READ
				&& method.getParameters().length == 0) {
			return cachingConfiguration.getTimeToLive();
		}
		return 0;
	}

}
