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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Servlet Filter that delegates to a {@link HttpTunnelServer}.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
public class HttpTunnelFilter implements Filter {
	private final ServerHttpRequestMatcher matcher;

	private final HttpTunnelServer server;

	public HttpTunnelFilter(ServerHttpRequestMatcher matcher, HttpTunnelServer server) {
		Assert.notNull(server, "Server must not be null");
		Assert.notNull(matcher, "matcher must not be null");
		this.matcher = matcher;
		this.server = server;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest
				&& response instanceof HttpServletResponse) {
			ServerHttpRequest serverRequest = new ServletServerHttpRequest((HttpServletRequest) request);
			if (this.matcher.matches(serverRequest)) {
				ServerHttpResponse serverResponse = new ServletServerHttpResponse((HttpServletResponse) response);
				this.server.handle(serverRequest,
						serverResponse);
				return;
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
	}

}
