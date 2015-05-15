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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertFalse;

/**
 * Tests for {@link SecuredServletServerHttpRequestMatcherTests}.
 *
 * @author Rob Winch
 * @since 1.3.0
 */
public class SecuredServletServerHttpRequestMatcherTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private String url;

	private String secretHeaderName;

	private String secret;

	private SecuredServerHttpRequestMatcher matcher;

	private MockHttpServletRequest request;

	@Before
	public void setup() {
		this.url = "/tunnel";
		this.secretHeaderName = "X-AUTH_TOKEN";
		this.secret = "secret";
		this.matcher = new SecuredServerHttpRequestMatcher(this.url,
				this.secretHeaderName, this.secret);
		this.request = new MockHttpServletRequest("GET", this.url);
		this.request.addHeader(this.secretHeaderName, this.secret);
	}

	@Test
	public void constructorUrlMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestURI must not be empty");
		new SecuredServerHttpRequestMatcher("", this.secretHeaderName, this.secret);
	}

	@Test
	public void constructorUrlMustStartWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RequestURI must start with '/'");
		new SecuredServerHttpRequestMatcher("tunnel", this.secretHeaderName, this.secret);
	}

	@Test
	public void constructorSecretHeaderNameMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("SecretHeaderName must not be empty");
		new SecuredServerHttpRequestMatcher(this.url, null, this.secret);
	}

	@Test
	public void constructorSecretHeaderNameMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("SecretHeaderName must not be empty");
		new SecuredServerHttpRequestMatcher(this.url, "", this.secret);
	}

	@Test
	public void constructorSecretMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ExpectedSecret must not be empty");
		new SecuredServerHttpRequestMatcher(this.url, this.secretHeaderName, null);
	}

	@Test
	public void constructorSecretMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ExpectedSecret must not be empty");
		new SecuredServerHttpRequestMatcher(this.url, this.secretHeaderName, "");
	}

	@Test
	public void matchesIgnoresDifferentUrl() throws Exception {
		this.request.setRequestURI(this.url + "invalid");
		assertFalse(this.matcher.matches(new ServletServerHttpRequest(this.request)));
	}

	@Test
	public void matchesIgnoresDifferentSecret() throws Exception {
		this.request = new MockHttpServletRequest("GET", this.url);
		this.request.addHeader(this.secretHeaderName, this.secret + "invalid");
		assertFalse(this.matcher.matches(new ServletServerHttpRequest(this.request)));
	}

	@Test
	public void matchesIgnoresNoSecret() throws Exception {
		this.request = new MockHttpServletRequest("GET", this.url);
		assertFalse(this.matcher.matches(new ServletServerHttpRequest(this.request)));
	}

}
