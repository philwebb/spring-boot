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

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * {@link AbstractHttpServiceRegistrar} to support
 * {@link HttpServiceScan @HttpServiceScan} for {@link HttpService @HttpService} annotated
 * interfaces.
 *
 * @author Phillip Webb
 */
class HttpServiceScanRegistrar extends AbstractHttpServiceRegistrar {

	@Override
	protected void registerHttpServices(HttpServiceRegistry registry, AnnotationMetadata importingClassMetadata) {
		registerHttpServices(registry.forGroup(this::getGroupName, this::getClientType), importingClassMetadata);
	}

	private void registerHttpServices(HttpServiceRegistry.GroupSpec registry,
			AnnotationMetadata importingClassMetadata) {
		MergedAnnotations annotations = importingClassMetadata.getAnnotations();
		annotations.stream(HttpServiceScan.Container.class)
			.flatMap(this::getContainedAnnotations)
			.forEach(annotation -> register(registry, importingClassMetadata, annotation));
		annotations.stream(HttpServiceScan.class)
			.forEach(annotation -> register(registry, importingClassMetadata, annotation));
	}

	private Stream<MergedAnnotation<HttpServiceScan>> getContainedAnnotations(
			MergedAnnotation<HttpServiceScan.Container> container) {
		return Arrays.stream(container.getAnnotationArray(MergedAnnotation.VALUE, HttpServiceScan.class));
	}

	private void register(HttpServiceRegistry.GroupSpec registry, AnnotationMetadata importingClassMetadata,
			MergedAnnotation<HttpServiceScan> annotation) {
		String[] basePackages = annotation.getStringArray("basePackages");
		Class<?>[] basePackageClasses = annotation.getClassArray("basePackageClasses");
		if (basePackages.length == 0 && basePackageClasses.length == 0) {
			basePackages = new String[] { ClassUtils.getPackageName(importingClassMetadata.getClassName()) };
		}
		registry.detectInBasePackages(this::isHttpService, basePackageClasses);
		registry.detectInBasePackages(this::isHttpService, basePackages);
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
