/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.tunnel.client;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link HeaderClientHttpRequestInterceptor}.
 *
 * @author Rob Winch
 * @since 1.3.0
 */
@RunWith(MockitoJUnitRunner.class)
public class HeaderClientHttpRequestInterceptorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private String headerName;

	private String headerValue;

	private HeaderClientHttpRequestInterceptor interceptor;

	private HttpRequest request;

	private byte[] body;

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private ClientHttpResponse response;

	private MockHttpServletRequest httpRequest;

	@Before
	public void setup() throws IOException {
		this.body = new byte[] {};
		this.httpRequest = new MockHttpServletRequest();
		this.request = new ServletServerHttpRequest(this.httpRequest);
		this.headerName = "X-AUTH-TOKEN";
		this.headerValue = "secret";
		given(this.execution.execute(this.request, this.body)).willReturn(this.response);
		this.interceptor = new HeaderClientHttpRequestInterceptor(this.headerName,
				this.headerValue);
	}

	@Test
	public void constructorNullHeaderName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderName must not be empty");
		new HeaderClientHttpRequestInterceptor(null, this.headerValue);
	}

	@Test
	public void constructorEmptyHeaderName() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderName must not be empty");
		new HeaderClientHttpRequestInterceptor("", this.headerValue);
	}

	@Test
	public void constructorNullHeaderValue() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderValue must not be empty");
		new HeaderClientHttpRequestInterceptor(this.headerName, null);
	}

	@Test
	public void constructorEmptyHeaderValue() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("HeaderValue must not be empty");
		new HeaderClientHttpRequestInterceptor(this.headerName, "");
	}

	@Test
	public void intercept() throws IOException {
		ClientHttpResponse result = this.interceptor.intercept(this.request, this.body,
				this.execution);
		assertThat(this.request.getHeaders().getFirst(this.headerName),
				equalTo(this.headerValue));
		assertThat(result, equalTo(this.response));
	}

}
