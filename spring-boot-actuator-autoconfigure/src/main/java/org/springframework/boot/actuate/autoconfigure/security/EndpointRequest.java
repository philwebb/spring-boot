/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Factory that can be used to create a {@link RequestMatcher} for actuator endpoint
 * locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class EndpointRequest {

	private EndpointRequest() {
	}

	/**
	 * Returns a matcher that includes all {@link Endpoint actuator endpoints}. The
	 * {@link EndpointRequestMatcher#excluding(Class...) excluding} method can be used to
	 * further remove specific endpoints if required. For example: <pre class="code">
	 * EndpointRequestMatcher.toAnyEndpoint().excluding(ShutdownEndpoint.class)
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher toAnyEndpoint() {
		throw new UnsupportedOperationException(); // FIXME
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequestMatcher.to(ShutdownEndpoint.class, HealthEndpoint.class)
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(Class<?>... endpoints) {
		return new EndpointRequestMatcher(endpoints);
	}

	/**
	 * Returns a matcher that includes the specified {@link Endpoint actuator endpoints}.
	 * For example: <pre class="code">
	 * EndpointRequestMatcher.to("shutdown", "health")
	 * </pre>
	 * @param endpoints the endpoints to include
	 * @return the configured {@link RequestMatcher}
	 */
	public static EndpointRequestMatcher to(String... endpoints) {
		return new EndpointRequestMatcher(endpoints);
	}

	/**
	 * The request matcher used to match against {@link Endpoint actuator endpoints}.
	 */
	public static class EndpointRequestMatcher implements RequestMatcher {

		public EndpointRequestMatcher(Class<?>[] endpoints) {
			throw new UnsupportedOperationException(); // FIXME
		}

		public EndpointRequestMatcher(String[] endpoints) {
			throw new UnsupportedOperationException(); // FIXME
		}

		EndpointRequestMatcher excluding(Class<?>... endpoints) {
			throw new UnsupportedOperationException(); // FIXME
		}

		EndpointRequestMatcher excluding(String... endpoints) {
			throw new UnsupportedOperationException(); // FIXME
		}

		@Override
		public boolean matches(HttpServletRequest request) {
			throw new UnsupportedOperationException(); // FIXME
		}

	}

}
