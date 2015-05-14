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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

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

/**
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
		body = new byte[] {};
		httpRequest = new MockHttpServletRequest();
		request = new ServletServerHttpRequest(httpRequest);
		headerName = "X-AUTH-TOKEN";
		headerValue = "secret";

		when(execution.execute(request, body)).thenReturn(response);

		interceptor = new HeaderClientHttpRequestInterceptor(headerName, headerValue);
	}

	@Test
	public void constructorNullHeaderName() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("headerName must not be null");

		new HeaderClientHttpRequestInterceptor(null, headerValue);
	}

	@Test
	public void constructorEmptyHeaderName() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("headerName must not be empty");

		new HeaderClientHttpRequestInterceptor("", headerValue);
	}

	@Test
	public void constructorNullHeaderValue() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("headerValue must not be null");

		new HeaderClientHttpRequestInterceptor(headerName, null);
	}

	@Test
	public void constructorEmptyHeaderValue() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("headerValue must not be empty");

		new HeaderClientHttpRequestInterceptor(headerName, "");
	}

	@Test
	public void intercept() throws IOException {
		ClientHttpResponse result = interceptor.intercept(request, body, execution);

		assertThat(request.getHeaders().getFirst(headerName), equalTo(headerValue));
		assertThat(result, equalTo(response));
	}
}
