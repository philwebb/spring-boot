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

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.service.scan.WithSpecificGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HttpServiceClientGroupMismatchException}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientGroupMismatchExceptionTests {

	@Test
	void throwOnMismatchThrowsOnMismatch() {
		assertThatExceptionOfType(HttpServiceClientGroupMismatchException.class)
			.isThrownBy(() -> HttpServiceClientGroupMismatchException.throwOnMismatch(WithSpecificGroup.class, "test",
					"spring"))
			.satisfies((ex) -> {
				assertThat(ex.getMessage()).isEqualTo("@HttpServiceClient group mismatch for interface "
						+ "org.springframework.boot.http.client.service.scan.WithSpecificGroup "
						+ "(requested 'test' but was registered with 'spring')");
				assertThat(ex.getServiceType()).isEqualTo(WithSpecificGroup.class);
				assertThat(ex.getRequestedGroup()).isEqualTo("test");
				assertThat(ex.getActualGroup()).isEqualTo("spring");
			});
	}

	@Test
	void throwOnMismatchDoesNothingOnMatch() {
		HttpServiceClientGroupMismatchException.throwOnMismatch(WithSpecificGroup.class, "test", "test");
	}

	@Test
	void throwOnMismatchIsCaseSensitive() {
		assertThatExceptionOfType(HttpServiceClientGroupMismatchException.class).isThrownBy(
				() -> HttpServiceClientGroupMismatchException.throwOnMismatch(WithSpecificGroup.class, "test", "Test"));
	}

}
