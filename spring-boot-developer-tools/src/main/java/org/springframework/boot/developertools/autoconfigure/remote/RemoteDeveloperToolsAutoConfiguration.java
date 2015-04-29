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
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelFilter;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelServer;
import org.springframework.boot.developertools.tunnel.server.RemoteDebugPortProvider;
import org.springframework.boot.developertools.tunnel.server.SocketTargetServerConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for remote development support.
 *
 * @author Phillip Webb
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.developertools.remote", name = "enabled")
@EnableConfigurationProperties(RemoteDeveloperToolsProperties.class)
public class RemoteDeveloperToolsAutoConfiguration {

	private static final Log logger = LogFactory
			.getLog(RemoteDeveloperToolsAutoConfiguration.class);

	/**
	 * Configuration for remote debug HTTP tunneling.
	 */
	@ConditionalOnClass(Filter.class)
	@Conditional(RemoteDebugCondition.class)
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
		public HttpTunnelFilter remoteDebugHttpTunnelFilter(
				@Qualifier("remoteDebugHttpTunnelServer") HttpTunnelServer server) {
			String url = this.properties.getContextPath() + "/debug";
			logger.info("Listening for remote debug traffic on " + url);
			return new HttpTunnelFilter(url, server);
		}

	}

	/**
	 * {@link Condition} to check if the application was started with remote debug.
	 */
	private static class RemoteDebugCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			if (RemoteDebugPortProvider.isRemoteDebugRunning()) {
				return ConditionOutcome.match("Remote Debug running");
			}
			return ConditionOutcome.noMatch("Remote debug not running");
		}

	}

}
