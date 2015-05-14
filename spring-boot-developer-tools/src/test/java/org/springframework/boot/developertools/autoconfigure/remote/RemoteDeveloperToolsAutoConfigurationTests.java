/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.boot.developertools.autoconfigure.remote;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelFilter;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelServer;
import org.springframework.boot.developertools.tunnel.server.RemoteDebugPortProvider;
import org.springframework.boot.developertools.tunnel.server.SocketTargetServerConnection;
import org.springframework.boot.developertools.tunnel.server.TargetServerConnection;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 *
 * @author Rob Winch
 * @since 1.3.0
 */
public class RemoteDeveloperToolsAutoConfigurationTests {

	AnnotationConfigWebApplicationContext context;

	MockHttpServletRequest request;
	MockHttpServletResponse response;
	MockFilterChain chain;

	@Before
	public void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		chain = new MockFilterChain();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultSetup() throws Exception {
		loadContext("spring.developertools.remote.secret:supersecret");

		HttpTunnelFilter filter = this.context.getBean(HttpTunnelFilter.class);

		request.setRequestURI(RemoteDeveloperToolsProperties.DEFAULT_CONTEXT_PATH + "/debug");
		request.addHeader(RemoteDeveloperToolsProperties.DEFAULT_SECRET_HEADER_NAME, "supersecret");

		filter.doFilter(request, response, chain);

		assertTunnel(true);
	}

	@Test
	public void invalidUrlInRequest() throws Exception {
		loadContext("spring.developertools.remote.secret:supersecret");

		HttpTunnelFilter filter = this.context.getBean(HttpTunnelFilter.class);

		request.setRequestURI("/debug");
		request.addHeader(RemoteDeveloperToolsProperties.DEFAULT_SECRET_HEADER_NAME, "supersecret");

		filter.doFilter(request, response, chain);

		assertTunnel(false);
	}

	@Test
	public void missingSecretInConfigDisables() throws Exception {
		loadContext("a:b");

		String[] namesForType = this.context.getBeanNamesForType(HttpTunnelFilter.class);

		assertThat(namesForType.length, equalTo(0));
	}

	@Test
	public void missingSecretFromRequest() throws Exception {
		loadContext("spring.developertools.remote.secret:supersecret");

		HttpTunnelFilter filter = this.context.getBean(HttpTunnelFilter.class);

		request.setRequestURI(RemoteDeveloperToolsProperties.DEFAULT_CONTEXT_PATH + "/debug");

		filter.doFilter(request, response, chain);

		assertTunnel(false);
	}

	@Test
	public void invalidSecretInRequest() throws Exception {
		loadContext("spring.developertools.remote.secret:supersecret");

		HttpTunnelFilter filter = this.context.getBean(HttpTunnelFilter.class);

		request.setRequestURI(RemoteDeveloperToolsProperties.DEFAULT_CONTEXT_PATH + "/debug");
		request.addHeader(RemoteDeveloperToolsProperties.DEFAULT_SECRET_HEADER_NAME, "invalid");

		filter.doFilter(request, response, chain);

		assertTunnel(false);
	}

	@Test
	public void customHeaderName() throws Exception {
		loadContext("spring.developertools.remote.secret:supersecret",
				"spring.developertools.remote.secretHeaderName:customheader");

		HttpTunnelFilter filter = this.context.getBean(HttpTunnelFilter.class);

		request.setRequestURI(RemoteDeveloperToolsProperties.DEFAULT_CONTEXT_PATH + "/debug");
		request.addHeader("customheader", "supersecret");

		filter.doFilter(request, response, chain);

		assertTunnel(true);
	}

	/**
	 * Asserts that the request tunneled through
	 *
	 * @param value
	 */
	private void assertTunnel(boolean value) {
		assertThat(this.context.getBean(MockHttpTunnelServer.class).invoked, equalTo(value));
	}

	private void loadContext(String...properties) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(Config.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, properties);
		this.context.refresh();
	}

	@Import(RemoteDeveloperToolsAutoConfiguration.class)
	@Configuration
	static class Config {

		@Bean
		public HttpTunnelServer remoteDebugHttpTunnelServer() {
			return new MockHttpTunnelServer(new SocketTargetServerConnection(
					new RemoteDebugPortProvider()));
		}
	}

	static class MockHttpTunnelServer extends HttpTunnelServer {
		private boolean invoked;

		public MockHttpTunnelServer(TargetServerConnection serverConnection) {
			super(serverConnection);
		}

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response)
				throws IOException {
			this.invoked = true;
		}
	}
}
