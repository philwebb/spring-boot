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

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MockServerRestTemplateCustomizer}.
 *
 * @author Phillip Webb
 */
public class MockServerRestTemplateCustomizerTests {

	@Test
	@Ignore
	public void test() {
		MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
		MyBean bean = new MyBean(new RestTemplateBuilder(customizer));
		customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
		bean.makeRestCall();
	}

	@Test
	public void testName() throws Exception {
		RestTemplate restTemplate1 = new RestTemplate();
		RestTemplate restTemplate2 = new RestTemplate();
		RequestExpectationManager manager = new SimpleRequestExpectationManager();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate1)
				.build(manager);
		MockRestServiceServer.bindTo(restTemplate2).build(manager);
		server.expect(requestTo("http://example.com/a")).andRespond(withSuccess());
		server.expect(requestTo("http://example.com/b")).andRespond(withSuccess());
		restTemplate1.getForObject("http://example.com/a", String.class);
		restTemplate2.getForObject("http://example.com/b", String.class);
	}

	private static class MyBean {

		public MyBean(RestTemplateBuilder restTemplateBuilder) {
		}

		public void makeRestCall() {
		}

	}

}
