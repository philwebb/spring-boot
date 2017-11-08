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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.boot.actuate.endpoint.OperationInvoker;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.ParameterMapper;
import org.springframework.boot.actuate.endpoint.ReflectiveOperationInvoker;
import org.springframework.boot.actuate.endpoint.annotation.AnnotatedEndpointOperationFactory;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.cache.CachingOperationInvoker;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebEndpointHttpMethod;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

final class WebEndpointOperationFactory
		implements AnnotatedEndpointOperationFactory<WebOperation> {

	private static final boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
			"org.reactivestreams.Publisher",
			WebEndpointOperationFactory.class.getClassLoader());

	private final ParameterMapper parameterMapper;

	private final EndpointMediaTypes endpointMediaTypes;

	private final EndpointPathResolver endpointPathResolver;

	WebEndpointOperationFactory(ParameterMapper parameterMapper,
			EndpointMediaTypes endpointMediaTypes,
			EndpointPathResolver endpointPathResolver) {
		this.parameterMapper = parameterMapper;
		this.endpointMediaTypes = endpointMediaTypes;
		this.endpointPathResolver = endpointPathResolver;
	}

	@Override
	public WebOperation createOperation(String endpointId,
			AnnotationAttributes operationAttributes, Object target, Method method,
			OperationType type, long timeToLive) {
		WebEndpointHttpMethod httpMethod = determineHttpMethod(type);
		OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
				determinePath(endpointId, method), httpMethod,
				determineConsumedMediaTypes(httpMethod, method),
				determineProducedMediaTypes(
						operationAttributes.getStringArray("produces"), method));
		OperationInvoker invoker = new ReflectiveOperationInvoker(target, method,
				this.parameterMapper);
		if (timeToLive > 0) {
			invoker = new CachingOperationInvoker(invoker, timeToLive);
		}
		return new WebOperation(type, invoker, determineBlocking(method),
				requestPredicate, determineId(endpointId, method));
	}

	private String determinePath(String endpointId, Method operationMethod) {
		StringBuilder path = new StringBuilder(
				this.endpointPathResolver.resolvePath(endpointId));
		Stream.of(operationMethod.getParameters())
				.filter((
						parameter) -> parameter.getAnnotation(Selector.class) != null)
				.map((parameter) -> "/{" + parameter.getName() + "}")
				.forEach(path::append);
		return path.toString();
	}

	private String determineId(String endpointId, Method operationMethod) {
		StringBuilder path = new StringBuilder(endpointId);
		Stream.of(operationMethod.getParameters())
				.filter((
						parameter) -> parameter.getAnnotation(Selector.class) != null)
				.map((parameter) -> "-" + parameter.getName()).forEach(path::append);
		return path.toString();
	}

	private Collection<String> determineConsumedMediaTypes(
			WebEndpointHttpMethod httpMethod, Method method) {
		if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
			return this.endpointMediaTypes.getConsumed();
		}
		return Collections.emptyList();
	}

	private Collection<String> determineProducedMediaTypes(String[] produces,
			Method method) {
		if (produces.length > 0) {
			return Arrays.asList(produces);
		}
		if (Void.class.equals(method.getReturnType())
				|| void.class.equals(method.getReturnType())) {
			return Collections.emptyList();
		}
		if (producesResourceResponseBody(method)) {
			return Collections.singletonList("application/octet-stream");
		}
		return this.endpointMediaTypes.getProduced();
	}

	private boolean producesResourceResponseBody(Method method) {
		if (Resource.class.equals(method.getReturnType())) {
			return true;
		}
		if (WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
			ResolvableType returnType = ResolvableType.forMethodReturnType(method);
			if (ResolvableType.forClass(Resource.class)
					.isAssignableFrom(returnType.getGeneric(0))) {
				return true;
			}
		}
		return false;
	}

	private boolean consumesRequestBody(Method method) {
		return Stream.of(method.getParameters()).anyMatch(
				(parameter) -> parameter.getAnnotation(Selector.class) == null);
	}

	private WebEndpointHttpMethod determineHttpMethod(OperationType operationType) {
		if (operationType == OperationType.WRITE) {
			return WebEndpointHttpMethod.POST;
		}
		if (operationType == OperationType.DELETE) {
			return WebEndpointHttpMethod.DELETE;
		}
		return WebEndpointHttpMethod.GET;
	}

	private boolean determineBlocking(Method method) {
		return !REACTIVE_STREAMS_PRESENT
				|| !Publisher.class.isAssignableFrom(method.getReturnType());
	}

}