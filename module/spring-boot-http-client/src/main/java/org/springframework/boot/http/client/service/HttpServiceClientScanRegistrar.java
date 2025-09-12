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

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.http.client.service.HttpServiceClientScan.Container;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * {@link ImportBeanDefinitionRegistrar} for
 * {@link HttpServiceClientScan @HttpServiceClientScan} or scanning of specific packages
 * for HTTP Service clients.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public class HttpServiceClientScanRegistrar extends AbstractHttpServiceRegistrar {

	/**
	 * The URI scheme used to indicate a group.
	 */
	public static final String GROUP_SCHEME = "group://";

	private final ClientType clientType;

	private final @Nullable Supplier<List<String>> basePackages;

	private @Nullable Environment environment;

	private @Nullable ResourceLoader resourceLoader;

	/**
	 * Create a new {@link HttpServiceClientScanRegistrar} to process
	 * {@link HttpServiceClientScan @HttpServiceClientScan} annotations.
	 */
	public HttpServiceClientScanRegistrar() {
		this.clientType = ClientType.UNSPECIFIED;
		this.basePackages = null;
	}

	/**
	 * Create a new {@link HttpServiceClientScanRegistrar} that scans the supplied base
	 * packages without processing {@link HttpServiceClientScan @HttpServiceClientScan}
	 * annotations.
	 * @param clientType the client type to use
	 * @param basePackages a supplier to provide the base packages to scan.
	 */
	public HttpServiceClientScanRegistrar(ClientType clientType, Supplier<List<String>> basePackages) {
		this.clientType = clientType;
		this.basePackages = basePackages;
	}

	@Override
	public void setEnvironment(Environment environment) {
		super.setEnvironment(environment);
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		this.resourceLoader = resourceLoader;
	}

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
		Scanner scanner = new Scanner(this.environment, this.resourceLoader);
		Processor processor = new Processor(registry, metadata, scanner);
		if (this.basePackages != null) {
			processor.process(this.clientType, this.basePackages.get());
			return;
		}
		MergedAnnotations annotations = metadata.getAnnotations();
		MergedAnnotation<?> container = annotations.get(Container.class);
		if (container.isPresent()) {
			processor.process(container.getAnnotationArray(MergedAnnotation.VALUE, HttpServiceClientScan.class));
		}
		processor.process(annotations.stream(HttpServiceClientScan.class));
	}

	/**
	 * Processes the {@link HttpServiceClientScan @HttpServiceClientScan} annotations.
	 */
	private record Processor(GroupRegistry registry, AnnotationMetadata metadata, Scanner scanner) {

		void process(ClientType clientType, List<String> basePackages) {
			for (String basePackge : basePackages) {
				processPackage(clientType, basePackge);
			}
		}

		void process(Stream<MergedAnnotation<HttpServiceClientScan>> annotations) {
			process(annotations.toArray(MergedAnnotation<?>[]::new));
		}

		void process(MergedAnnotation<?>[] annotations) {
			for (MergedAnnotation<?> annotation : annotations) {
				process(annotation);
			}
		}

		private void process(MergedAnnotation<?> annotation) {
			String[] basePackages = annotation.getStringArray("basePackages");
			Class<?>[] basePackageClasses = annotation.getClassArray("basePackageClasses");
			ClientType clientType = annotation.getEnum("clientType", ClientType.class);
			if (ObjectUtils.isEmpty(basePackages) && ObjectUtils.isEmpty(basePackageClasses)) {
				basePackages = new String[] { ClassUtils.getPackageName(metadata().getClassName()) };
			}
			for (String basePackage : basePackages) {
				processPackage(clientType, basePackage);
			}
			for (Class<?> basePackage : basePackageClasses) {
				processPackage(clientType, basePackage.getPackageName());
			}
		}

		private void processPackage(ClientType clientType, String basePackage) {
			for (BeanDefinition definition : this.scanner.findCandidateComponents(basePackage)) {
				AnnotationMetadata beanMetadata = ((AnnotatedBeanDefinition) definition).getMetadata();
				MergedAnnotation<?> annotation = beanMetadata.getAnnotations().get(HttpServiceClient.class);
				String group = scanner().getEnvironment().resolvePlaceholders(annotation.getString("group"));
				registry().forGroup(group, clientType).registerTypeNames(beanMetadata.getClassName());
			}
		}

	}

	/**
	 * Scans for suitable {@link HttpExchange @HttpExchange} interfaces.
	 */
	private static class Scanner extends ClassPathScanningCandidateComponentProvider {

		Scanner(@Nullable Environment environment, @Nullable ResourceLoader resourceLoader) {
			Assert.state(environment != null, "No 'environment' set");
			Assert.state(resourceLoader != null, "No 'resourceLoader' set");
			setEnvironment(environment);
			setResourceLoader(resourceLoader);
		}

		@Override
		protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
			AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
			return metadata.isIndependent() && !metadata.isAnnotation()
					&& metadata.getAnnotations().isPresent(HttpServiceClient.class);
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			return true;
		}

	}

}
