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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import org.springframework.boot.http.client.service.scan.TestHttpServiceClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer.GroupCallback;
import org.springframework.web.service.registry.HttpServiceGroupConfigurer.Groups;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link HttpServiceClientGroupVerifier}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientGroupVerifierTests {

	private final HttpServiceClientGroupVerifier<Object> verifier = new TestHttpServiceClientGroupVerifier();

	private final Object clientBuilder = mock();

	private final HttpServiceProxyFactory.Builder factoryBuilder = mock();

	@Test
	void configureGroupsWhenHasMismatchThrowsException() {
		assertThatExceptionOfType(HttpServiceClientGroupMismatchException.class)
			.isThrownBy(() -> this.verifier.configureGroups(mockGroups("spring", WithAnnotationInTestGroup.class)));
	}

	@Test
	void configureGroupsWhenHasMetaAnnoatedMismatchThrowsException() {
		assertThatExceptionOfType(HttpServiceClientGroupMismatchException.class)
			.isThrownBy(() -> this.verifier.configureGroups(mockGroups("spring", WithMetaAnnotationInTestGroup.class)));
	}

	@Test
	void configureGroupsWhenNoMismatchDoesNothing() {
		this.verifier.configureGroups(mockGroups("test", WithAnnotationInTestGroup.class));
	}

	@Test
	void configureGroupsWhenNoAnnotationDoesNothing() {
		this.verifier.configureGroups(mockGroups("spring", WithoutAnnotation.class));
	}

	private Groups<Object> mockGroups(String actualGroup, Class<?> serviceType) {
		Groups<Object> groups = mock();
		HttpServiceGroup group = mock();
		given(group.name()).willReturn(actualGroup);
		given(group.httpServiceTypes()).willReturn(Set.of(serviceType));
		will((invocation) -> invokeWithGroup(invocation, group)).given(groups).forEachGroup(any());
		return groups;
	}

	private Object invokeWithGroup(InvocationOnMock invocation, HttpServiceGroup group) {
		GroupCallback<Object> callback = invocation.getArgument(0);
		callback.withGroup(group, HttpServiceClientGroupVerifierTests.this.clientBuilder,
				HttpServiceClientGroupVerifierTests.this.factoryBuilder);
		return null;
	}

	@HttpServiceClient("test")
	static class WithAnnotationInTestGroup {

	}

	static class WithoutAnnotation {

	}

	@TestHttpServiceClient
	static class WithMetaAnnotationInTestGroup {

	}

	static class TestHttpServiceClientGroupVerifier extends HttpServiceClientGroupVerifier<Object> {

	}

}
