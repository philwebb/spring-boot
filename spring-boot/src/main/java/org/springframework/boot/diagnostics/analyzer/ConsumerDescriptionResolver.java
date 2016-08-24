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

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.util.ClassUtils;

/**
 * Provide a human readable description of the component that attempted
 * to inject a collaborator that couldn't be resolved.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.4.1
 */
public final class ConsumerDescriptionResolver {

	private ConsumerDescriptionResolver() {

	}

	/**
	 * Create a description of the component that lead to an exception.
	 *
	 * @param rootFailure the root exception
	 * @return a description of the component or {@code null}
	 */
	public static String getConsumerDescription(Throwable rootFailure) {
		UnsatisfiedDependencyException unsatisfiedDependency = findUnsatisfiedDependencyException(
				rootFailure);
		if (unsatisfiedDependency != null) {
			return getConsumerDescription(unsatisfiedDependency);
		}
		BeanInstantiationException beanInstantiationException = findBeanInstantiationException(
				rootFailure);
		if (beanInstantiationException != null) {
			return getConsumerDescription(beanInstantiationException);
		}
		return null;
	}

	private static String getConsumerDescription(UnsatisfiedDependencyException ex) {
		InjectionPoint injectionPoint = ex.getInjectionPoint();
		if (injectionPoint != null) {
			if (injectionPoint.getField() != null) {
				return String.format("Field %s in %s",
						injectionPoint.getField().getName(),
						injectionPoint.getField().getDeclaringClass().getName());
			}
			if (injectionPoint.getMethodParameter() != null) {
				if (injectionPoint.getMethodParameter().getConstructor() != null) {
					return String.format("Parameter %d of constructor in %s",
							injectionPoint.getMethodParameter().getParameterIndex(),
							injectionPoint.getMethodParameter().getDeclaringClass()
									.getName());
				}
				return String.format("Parameter %d of method %s in %s",
						injectionPoint.getMethodParameter().getParameterIndex(),
						injectionPoint.getMethodParameter().getMethod().getName(),
						injectionPoint.getMethodParameter().getDeclaringClass()
								.getName());
			}
		}
		return ex.getResourceDescription();
	}

	private static String getConsumerDescription(BeanInstantiationException ex) {
		if (ex.getConstructingMethod() != null) {
			return String.format("Method %s in %s", ex.getConstructingMethod().getName(),
					ex.getConstructingMethod().getDeclaringClass().getName());
		}
		if (ex.getConstructor() != null) {
			return String.format("Constructor in %s", ClassUtils
					.getUserClass(ex.getConstructor().getDeclaringClass()).getName());
		}
		return ex.getBeanClass().getName();
	}

	private static UnsatisfiedDependencyException findUnsatisfiedDependencyException(
			Throwable root) {
		return findMostNestedCause(root, UnsatisfiedDependencyException.class);
	}

	private static BeanInstantiationException findBeanInstantiationException(Throwable root) {
		return findMostNestedCause(root, BeanInstantiationException.class);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Exception> T findMostNestedCause(Throwable root,
			Class<T> causeType) {
		Throwable candidate = root;
		T mostNestedMatch = null;
		while (candidate != null) {
			if (causeType.isAssignableFrom(candidate.getClass())) {
				mostNestedMatch = (T) candidate;
			}
			candidate = candidate.getCause();
		}
		return mostNestedMatch;
	}

}
