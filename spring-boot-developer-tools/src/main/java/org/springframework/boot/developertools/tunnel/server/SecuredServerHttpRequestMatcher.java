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

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.Assert;

/**
 * Creates a matcher that matches on the requestURI and requires a secret to be present in
 * the header.
 *
 * @author Rob Winch
 * @since 1.3.0
 */
public class SecuredServerHttpRequestMatcher implements ServerHttpRequestMatcher {

	private final String requestUri;

	private final String secretHeaderName;

	private final String expectedSecret;

	/**
	 * Creates a new {@link SecuredServerHttpRequestMatcher} instance.
	 * @param requestUri the requestUri to match on.
	 * @param secretHeaderName the name of the header that must contain the secret.
	 * @param expectedSecret the expected value of the header (i.e. a password)
	 */
	public SecuredServerHttpRequestMatcher(String requestUri, String secretHeaderName,
			String expectedSecret) {
		Assert.hasLength(requestUri, "RequestURI must not be empty");
		Assert.isTrue(requestUri.startsWith("/"), "RequestURI must start with '/'");
		Assert.hasLength(secretHeaderName, "SecretHeaderName must not be empty");
		Assert.hasLength(expectedSecret, "ExpectedSecret must not be empty");
		this.requestUri = requestUri;
		this.secretHeaderName = secretHeaderName;
		this.expectedSecret = expectedSecret;
	}

	@Override
	public boolean matches(ServerHttpRequest request) {
		if (!this.requestUri.equals(request.getURI().getPath())) {
			return false;
		}
		String providedSecret = request.getHeaders().getFirst(this.secretHeaderName);
		return this.expectedSecret.equals(providedSecret);
	}
}
