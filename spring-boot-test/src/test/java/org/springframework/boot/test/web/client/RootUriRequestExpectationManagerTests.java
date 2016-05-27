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

package org.springframework.boot.test.web.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RootUriRequestExpectationManager}.
 *
 * @author Phillip Webb
 */
public class RootUriRequestExpectationManagerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void boundRestTemplateShouldPrefixRootUri() {
		RestTemplate restTemplate = new RestTemplateBuilder()
				.rootUri("http://example.com").build();
		MockRestServiceServer server = RootUriRequestExpectationManager
				.bindTo(restTemplate);
		server.expect(requestTo("/hello")).andRespond(withSuccess());
		restTemplate.getForEntity("/hello", String.class);
	}

	@Test
	public void boundRestTemplateWhenUrlIncludesDomainShouldNotPrefixRootUri() {
		RestTemplate restTemplate = new RestTemplateBuilder()
				.rootUri("http://example.com").build();
		MockRestServiceServer server = RootUriRequestExpectationManager
				.bindTo(restTemplate);
		server.expect(requestTo("/hello")).andRespond(withSuccess());
		this.thrown.expect(AssertionError.class);
		this.thrown.expectMessage(
				"expected:<http://example.com/hello> but was:<http://spring.io/hello>");
		restTemplate.getForEntity("http://spring.io/hello", String.class);
	}

	// FIXME test

}
