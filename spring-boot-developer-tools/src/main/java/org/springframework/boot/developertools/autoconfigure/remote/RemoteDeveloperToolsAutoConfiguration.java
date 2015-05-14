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

package org.springframework.boot.developertools.autoconfigure.remote;

import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelFilter;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelServer;
import org.springframework.boot.developertools.tunnel.server.RemoteDebugPortProvider;
import org.springframework.boot.developertools.tunnel.server.SecuredServerHttpRequestMatcher;
import org.springframework.boot.developertools.tunnel.server.ServerHttpRequestMatcher;
import org.springframework.boot.developertools.tunnel.server.SocketTargetServerConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 * @author Rob Winch
 * @since 1.3.0
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.developertools.remote", name = "secret")
@EnableConfigurationProperties(RemoteDeveloperToolsProperties.class)
public class RemoteDeveloperToolsAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(RemoteDeveloperToolsAutoConfiguration.class);

	/**
	 * Configuration for remote debug HTTP tunneling.
	 */
	@ConditionalOnClass(Filter.class)
	@ConditionalOnProperty(prefix = "spring.developertools.remote.debug", name = "enabled", matchIfMissing = true)
	static class RemoteDebugTunnelConfiguration {

		@Autowired
		private RemoteDeveloperToolsProperties properties;

		@Bean
		@ConditionalOnMissingBean(name = "remoteDebugHttpTunnelServer")
		public HttpTunnelServer remoteDebugHttpTunnelServer() {
			return new HttpTunnelServer(new SocketTargetServerConnection(
					new RemoteDebugPortProvider()));
		}

		@Bean
		@ConditionalOnMissingBean(name = "remoteDebugServerHttpRequestMatcher")
		public ServerHttpRequestMatcher remoteDebugServerHttpRequestMatcher() {
			String url = this.properties.getContextPath() + "/debug";
			logger.info("Listening for remote debug traffic on " + url);
			String secret = this.properties.getSecret();
			String secretHeader = this.properties.getSecretHeaderName();
			return new SecuredServerHttpRequestMatcher(url, secretHeader, secret);
		}


		@Bean
		public HttpTunnelFilter remoteDebugHttpTunnelFilter(@Qualifier("remoteDebugServerHttpRequestMatcher") ServerHttpRequestMatcher matcher,
				@Qualifier("remoteDebugHttpTunnelServer") HttpTunnelServer server) {
			String url = this.properties.getContextPath() + "/debug";
			logger.info("Listening for remote debug traffic on " + url);
			return new HttpTunnelFilter(matcher, server);
		}

	}

}
