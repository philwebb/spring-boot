/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.service.invoker;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * {@link AbstractHttpServiceRegistrar} for {@link HttpService @HttpService} annotated
 * classes found in {@link AutoConfigurationPackages}.
 *
 * @author Phillip Webb
 */
class AutoConfigurationPackagesHttpServiceScanRegistrar extends AbstractHttpServiceRegistrar {

	private final BeanFactory beanFactory;

	AutoConfigurationPackagesHttpServiceScanRegistrar(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata importingClassMetadata) {
		registerHttpServices(registry.forGroup(this::getGroupName, this::getClientType), importingClassMetadata);
	}

	private void registerHttpServices(HttpServiceRegistry.GroupSpec registry,
			AnnotationMetadata importingClassMetadata) {
		if (AutoConfigurationPackages.has(this.beanFactory)) {
			AutoConfigurationPackages.get(this.beanFactory)
				.forEach((basePackage) -> registry.detectInBasePackages(this::isHttpService, basePackage));
		}
	}

	private boolean isHttpService(AnnotationMetadata metadata) {
		return metadata.getAnnotations().isPresent(HttpService.class);
	}

	private String getGroupName(Class<?> type) {
		return MergedAnnotations.from(type).get(HttpService.class).getString("group");
	}

	private ClientType getClientType(Class<?> type) {
		return MergedAnnotations.from(type).get(HttpService.class).getEnum("clientType", ClientType.class);
	}

}
