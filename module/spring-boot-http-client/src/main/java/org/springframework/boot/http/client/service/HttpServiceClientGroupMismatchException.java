/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.service;

import org.springframework.util.ObjectUtils;

/**
 * Exception thrown when a {@link HttpServiceClient @HttpServiceClient} annotated
 * interface is registered with an incorrect group.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class HttpServiceClientGroupMismatchException extends IllegalStateException {

	private final Class<?> serviceType;

	private final String requestedGroup;

	private final String actualGroup;

	private HttpServiceClientGroupMismatchException(Class<?> serviceType, String requestedGroup, String actualGroup) {
		super(buildMessage(serviceType, requestedGroup, actualGroup));
		this.serviceType = serviceType;
		this.requestedGroup = requestedGroup;
		this.actualGroup = actualGroup;
	}

	/**
	 * Return the HTTP Service type.
	 * @return the service type
	 */
	public Class<?> getServiceType() {
		return this.serviceType;
	}

	/**
	 * Return the group that was requested by the
	 * {@link HttpServiceClient @HttpServiceClient} annotation.
	 * @return the requested group
	 */
	public String getRequestedGroup() {
		return this.requestedGroup;
	}

	/**
	 * Return the group where the service was actually registered.
	 * @return the actual group
	 */
	public String getActualGroup() {
		return this.actualGroup;
	}

	private static String buildMessage(Class<?> serviceType, String requestedGroup, String actualGroup) {
		return String.format("@HttpServiceClient group mismatch for %s (requested '%s' "
				+ "but was registered with '%s'). Classes annoated with @HttpServiceClient "
				+ "should not be directly registered.", serviceType, requestedGroup, actualGroup);
	}

	static void throwOnMismatch(Class<?> serviceType, String requestedGroup, String actualGroup) {
		if (!ObjectUtils.nullSafeEquals(requestedGroup, actualGroup)) {
			throw new HttpServiceClientGroupMismatchException(serviceType, requestedGroup, actualGroup);
		}
	}

}
