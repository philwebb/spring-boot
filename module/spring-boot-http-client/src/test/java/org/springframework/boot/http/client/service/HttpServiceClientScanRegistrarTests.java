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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.http.client.service.scan.ScanConfiguration;
import org.springframework.boot.http.client.service.scan.WithDefaultGroup;
import org.springframework.boot.http.client.service.scan.WithEnvironmentVariable;
import org.springframework.boot.http.client.service.scan.WithMetaAnnotation;
import org.springframework.boot.http.client.service.scan.WithSpecificGroup;
import org.springframework.boot.http.client.service.scan.WithoutHttpServiceClientAnnotation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.style.ToStringCreator;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceClientScanRegistrar} and
 * {@link HttpServiceClientScan @HttpServiceClientScan}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientScanRegistrarTests {

	@Test
	void scanWhenHasBasePackages() {
		assertRegistrations(BasePackagesConfiguration.class);
	}

	@Test
	void scanWhenHasBasePackageClasses() {
		assertRegistrations(BasePackageClassesConfiguration.class);
	}

	@Test
	void scanWhenHasNoExplicitPackages() {
		assertRegistrations(ScanConfiguration.class);
	}

	private void assertRegistrations(Class<?> configuration) {
		Map<String, Registration> registrations = getRegistrations(configuration);
		assertThat(registrations).containsOnlyKeys("default", "test", "fromenv");
		Registration defaultGroup = registrations.get("default");
		Registration testGroup = registrations.get("test");
		Registration fromEnvGroup = registrations.get("fromenv");
		assertThat(defaultGroup.clientType()).isEqualTo(ClientType.UNSPECIFIED);
		assertThat(defaultGroup.httpServiceTypeNames()).containsExactlyInAnyOrder(WithDefaultGroup.class.getName());
		assertThat(testGroup.clientType()).isEqualTo(ClientType.UNSPECIFIED);
		assertThat(testGroup.httpServiceTypeNames()).containsExactlyInAnyOrder(WithSpecificGroup.class.getName(),
				WithMetaAnnotation.class.getName());
		assertThat(fromEnvGroup.clientType()).isEqualTo(ClientType.UNSPECIFIED);
		assertThat(fromEnvGroup.httpServiceTypeNames())
			.containsExactlyInAnyOrder(WithEnvironmentVariable.class.getName());
	}

	private Map<String, Registration> getRegistrations(Class<?> configuration) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			MockPropertySource propertySource = new MockPropertySource();
			propertySource.setProperty("clientgroup", "fromenv");
			if (propertySource != null) {
				context.getEnvironment().getPropertySources().addLast(propertySource);
			}
			context.register(configuration);
			context.refreshForAotProcessing(new RuntimeHints());
			return getRegistrations(context);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Registration> getRegistrations(AnnotationConfigApplicationContext context) {
		BeanDefinition definition = context
			.getBeanDefinition(AbstractHttpServiceRegistrar.HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME);
		Object groupsMetadata = definition.getConstructorArgumentValues().getArgumentValue(0, Object.class).getValue();
		Map<String, Object> groupsMap = (Map<String, Object>) ReflectionTestUtils.getField(groupsMetadata, "groupMap");
		Map<String, Registration> registrations = new LinkedHashMap<>();
		groupsMap.forEach((key, value) -> registrations.put(key, new Registration(value)));
		return registrations;
	}

	static class Registration {

		private final Object target;

		Registration(Object target) {
			this.target = target;
		}

		String name() {
			return ReflectionTestUtils.invokeMethod(this.target, "name");
		}

		ClientType clientType() {
			return ReflectionTestUtils.invokeMethod(this.target, "clientType");
		}

		Set<String> httpServiceTypeNames() {
			return ReflectionTestUtils.invokeMethod(this.target, "httpServiceTypeNames");
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("name", name())
				.append("clientType", clientType())
				.append("httpServiceTypeNames", httpServiceTypeNames())
				.toString();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@HttpServiceClientScan("org.springframework.boot.http.client.service.scan")
	static class BasePackagesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@HttpServiceClientScan(basePackageClasses = WithoutHttpServiceClientAnnotation.class)
	static class BasePackageClassesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ImportDefinedPackagesAndClientType.class)
	static class DefinedPackagesAndClientTypeConfiguration {

	}

	static class ImportDefinedPackagesAndClientType extends HttpServiceClientScanRegistrar {

		ImportDefinedPackagesAndClientType() {
			super(ClientType.REST_CLIENT, () -> List.of("org.springframework.boot.http.client.service.scan"));
		}

	}

}
