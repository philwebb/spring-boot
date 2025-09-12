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

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpServiceClientGroupMismatchFailureAnalyzer}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientGroupMismatchFailureAnalyzerTests {

	@Test
	void shouldAnalyze() {
		FailureAnalysis analysis = analyze();
		assertThat(analysis.getDescription()).isEqualTo("""
				The @HttpServiceClient annotated interface '%s'
				has been registered to an incorrect group:

				    Requested: 'therequested' (from @HttpServiceClient)
				    Actual: 'theactual'

				Ensure that the interface has not be direcly registered by an @ImportHttpServices annotation
				and has not been imported by an AbstractHttpServiceRegistrar.""".formatted(TestClient.class.getName()));
		assertThat(analysis.getAction()).isEqualTo(
				"""
						Update your code to ensure '%s' is registered to the correct group by
						either removing it from any direct HTTP Service registration, or deleting the @HttpServiceClient annotation."""
					.formatted(TestClient.class.getName()));
	}

	private FailureAnalysis analyze() {
		HttpServiceClientGroupMismatchException failure = new HttpServiceClientGroupMismatchException(TestClient.class,
				"therequested", "theactual");
		return new HttpServiceClientGroupMismatchFailureAnalyzer().analyze(failure);
	}

	interface TestClient {

	}

}
