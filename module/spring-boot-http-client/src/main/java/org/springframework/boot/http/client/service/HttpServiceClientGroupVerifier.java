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

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer;

/**
 * Base class for {@link HttpServiceGroupConfigurer} implementations that verify classes
 * annotated with {@link HttpServiceClient @HttpServiceClient} end up in the correct
 * group.
 *
 * @param <C> the client builder type
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract class HttpServiceClientGroupVerifier<C> implements HttpServiceGroupConfigurer<C> {

	@Override
	public void configureGroups(Groups<C> groups) {
		groups.forEachGroup(this::verify);
	}

	void verify(HttpServiceGroup group, C clientBuilder, HttpServiceProxyFactory.Builder factoryBuilder) {
		group.httpServiceTypes().forEach((type) -> verify(type, group.name()));
	}

	private void verify(Class<?> serviceType, String actualGroup) {
		MergedAnnotation<HttpServiceClient> annotation = MergedAnnotations.from(serviceType)
			.get(HttpServiceClient.class);
		if (annotation.isPresent()) {
			String requestedGroup = annotation.getString("group");
			HttpServiceClientGroupMismatchException.throwOnMismatch(serviceType, requestedGroup, actualGroup);
		}
	}

}
