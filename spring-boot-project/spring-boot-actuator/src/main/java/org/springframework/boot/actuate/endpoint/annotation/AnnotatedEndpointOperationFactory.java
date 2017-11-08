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

import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * Factory to creates an {@link Operation} for an annotated method on an
 * {@link Endpoint @Endpoint}.
 *
 * @param <T> the {@link Operation} type
 */
@FunctionalInterface
public interface AnnotatedEndpointOperationFactory<T extends Operation> {

	/**
	 * Creates an {@code EndpointOperation} for an operation on an endpoint.
	 * @param endpointId the id of the endpoint
	 * @param annotationAttributes the annotation attributes for the operation
	 * @param target the target that implements the operation
	 * @param method the method on the bean that implements the operation
	 * @param operationType the type of the operation
	 * @param timeToLive the caching period in milliseconds
	 * @return the operation info that describes the operation
	 */
	T createOperation(String endpointId, AnnotationAttributes annotationAttributes,
			Object target, Method method, OperationType operationType, long timeToLive);

}
