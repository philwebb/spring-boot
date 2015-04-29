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

package org.springframework.boot.developertools.remote;

import java.nio.channels.SocketChannel;

import javax.net.ServerSocketFactory;
import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.developertools.autoconfigure.remote.RemoteDeveloperToolsProperties;
import org.springframework.boot.developertools.remote.RemoteDebugHttpTunnelClientConfiguration.PortAvailableCondition;
import org.springframework.boot.developertools.tunnel.client.HttpTunnelConnection;
import org.springframework.boot.developertools.tunnel.client.TunnelClient;
import org.springframework.boot.developertools.tunnel.client.TunnelClientListener;
import org.springframework.boot.developertools.tunnel.client.TunnelConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Configuration for remote debug client.
 *
 * @author Phillip Webb
 */
@ConditionalOnProperty(prefix = "spring.developertools.remote.debug", name = "enabled", matchIfMissing = true)
@ConditionalOnClass(Filter.class)
@EnableConfigurationProperties(RemoteDeveloperToolsProperties.class)
@Conditional(PortAvailableCondition.class)
public class RemoteDebugHttpTunnelClientConfiguration {

	private static Log logger = LogFactory
			.getLog(RemoteDebugHttpTunnelClientConfiguration.class);

	@Autowired
	private RemoteDeveloperToolsProperties properties;

	@Value("${remoteUrl}")
	private String remoteUrl;

	@Bean
	public TunnelClient remoteDebugTunnelClient() {
		String url = this.remoteUrl + this.properties.getContextPath() + "/debug";
		TunnelConnection tunnelConnection = new HttpTunnelConnection(url);
		int localPort = this.properties.getDebug().getLocalPort();
		TunnelClient client = new TunnelClient(localPort, tunnelConnection);
		client.addListener(new InfoLogger());
		return client;
	}

	private static class InfoLogger implements TunnelClientListener {

		@Override
		public void onOpen(SocketChannel socket) {
			logger.info("Remote debug connection established");
		}

		@Override
		public void onClose(SocketChannel socket) {
			logger.info("Remote debug connection close");
		}

	}

	/**
	 * Condition used to check that the actual local port is available.
	 */
	static class PortAvailableCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
					context.getEnvironment(), "spring.developertools.remote.debug.");
			Integer port = resolver.getProperty("local-port", Integer.class);
			if (port == null) {
				port = RemoteDeveloperToolsProperties.Debug.DEFAULT_LOCAL_PORT;
			}
			if (isPortAvailable(port)) {
				return ConditionOutcome.match("Debug local port availble");
			}
			return ConditionOutcome.noMatch("Debug local port not availble");
		}

		private boolean isPortAvailable(int port) {
			try {
				ServerSocketFactory.getDefault().createServerSocket(port).close();
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

	}

}
