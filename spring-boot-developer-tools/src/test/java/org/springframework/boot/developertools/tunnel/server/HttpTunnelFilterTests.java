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

package org.springframework.boot.developertools.tunnel.server;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link HttpTunnelFilter}.
 *
 * @author Phillip Webb
 */
public class HttpTunnelFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private HttpTunnelServer server;

	@Mock
	private FilterChain chain;

	@Captor
	private ArgumentCaptor<ServerHttpRequest> requestCaptor;

	@Captor
	private ArgumentCaptor<ServerHttpResponse> responseCaptor;

	private HttpTunnelFilter filter;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.filter = new HttpTunnelFilter("/tunnel", this.server);
	}

	@Test
	public void urlMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be null");
		new HttpTunnelFilter(null, this.server);
	}

	@Test
	public void urlMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new HttpTunnelFilter("", mock(HttpTunnelServer.class));
	}

	@Test
	public void urlMustStartWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must start with '/'");
		new HttpTunnelFilter("tunnel", mock(HttpTunnelServer.class));
	}

	@Test
	public void serverMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Server must not be null");
		new HttpTunnelFilter("/tunnel", null);
	}

	@Test
	public void ignoresNotServletRequests() throws Exception {
		ServletRequest request = mock(ServletRequest.class);
		ServletResponse response = mock(ServletResponse.class);
		this.filter.doFilter(request, response, this.chain);
		verifyZeroInteractions(this.server);
		verify(this.chain).doFilter(request, response);
	}

	@Test
	public void ignoresToDifferentUrl() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/hello");
		HttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, this.chain);
		verifyZeroInteractions(this.server);
		verify(this.chain).doFilter(request, response);
	}

	@Test
	public void handleUrl() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest("GET", "/tunnel");
		HttpServletResponse response = new MockHttpServletResponse();
		this.filter.doFilter(request, response, this.chain);
		verify(this.server).handle(this.requestCaptor.capture(),
				this.responseCaptor.capture());
		ServerHttpRequest httpRequest = this.requestCaptor.getValue();
		ServerHttpResponse httpResponse = this.responseCaptor.getValue();
		ServletServerHttpRequest serverRequest = (ServletServerHttpRequest) httpRequest;
		ServletServerHttpResponse serverResponse = (ServletServerHttpResponse) httpResponse;
		assertThat(serverRequest.getServletRequest(), equalTo(request));
		assertThat(serverResponse.getServletResponse(), equalTo(response));
		verifyZeroInteractions(this.chain);
	}

}
