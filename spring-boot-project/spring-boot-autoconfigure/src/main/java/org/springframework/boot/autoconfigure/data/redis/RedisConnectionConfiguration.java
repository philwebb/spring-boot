/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.redis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection.Cluster;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection.Node;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection.Sentinel;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base Redis connection configuration.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @author Alen Turkovic
 * @author Scott Frederick
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
abstract class RedisConnectionConfiguration {

	private static final boolean COMMONS_POOL2_AVAILABLE = ClassUtils.isPresent("org.apache.commons.pool2.ObjectPool",
			RedisConnectionConfiguration.class.getClassLoader());

	private final RedisProperties properties;

	private final RedisStandaloneConfiguration standaloneConfiguration;

	private final RedisSentinelConfiguration sentinelConfiguration;

	private final RedisClusterConfiguration clusterConfiguration;

	protected final RedisServiceConnection serviceConnection;

	protected RedisConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
			ObjectProvider<RedisServiceConnection> serviceConnectionProvider) {
		this.properties = properties;
		this.standaloneConfiguration = standaloneConfigurationProvider.getIfAvailable();
		this.sentinelConfiguration = sentinelConfigurationProvider.getIfAvailable();
		this.clusterConfiguration = clusterConfigurationProvider.getIfAvailable();
		this.serviceConnection = serviceConnectionProvider.getIfAvailable();
	}

	protected final RedisStandaloneConfiguration getStandaloneConfig() {
		if (this.standaloneConfiguration != null) {
			return this.standaloneConfiguration;
		}
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		if (this.serviceConnection == null && StringUtils.hasText(this.properties.getUrl())) {
			ConnectionInfo connectionInfo = parseUrl(this.properties.getUrl());
			config.setHostName(connectionInfo.getHostName());
			config.setPort(connectionInfo.getPort());
			config.setUsername(connectionInfo.getUsername());
			config.setPassword(RedisPassword.of(connectionInfo.getPassword()));
		}
		else {
			String host = (this.serviceConnection != null) ? this.serviceConnection.getStandalone().getHost()
					: this.properties.getHost();
			String username = (this.serviceConnection != null) ? this.serviceConnection.getUsername()
					: this.properties.getUsername();
			String password = (this.serviceConnection != null) ? this.serviceConnection.getPassword()
					: this.properties.getPassword();
			int port = (this.serviceConnection != null) ? this.serviceConnection.getStandalone().getPort()
					: this.properties.getPort();
			config.setHostName(host);
			config.setPort(port);
			config.setUsername(username);
			config.setPassword(RedisPassword.of(password));
		}
		int database = (this.serviceConnection != null) ? this.serviceConnection.getStandalone().getDatabase()
				: this.properties.getDatabase();
		config.setDatabase(database);
		return config;
	}

	protected final RedisSentinelConfiguration getSentinelConfig() {
		if (this.sentinelConfiguration != null) {
			return this.sentinelConfiguration;
		}
		RedisProperties.Sentinel sentinelProperties = this.properties.getSentinel();
		if (sentinelProperties != null
				|| (this.serviceConnection != null && this.serviceConnection.getSentinel() != null)) {
			RedisSentinelConfiguration config = new RedisSentinelConfiguration();
			String master = (this.serviceConnection != null) ? this.serviceConnection.getSentinel().getMaster()
					: sentinelProperties.getMaster();
			config.master(master);
			List<RedisNode> sentinels = (this.serviceConnection != null)
					? createSentinels(this.serviceConnection.getSentinel()) : createSentinels(sentinelProperties);
			config.setSentinels(sentinels);
			String username = (this.serviceConnection != null) ? this.serviceConnection.getUsername()
					: this.properties.getUsername();
			String password = (this.serviceConnection != null) ? this.serviceConnection.getPassword()
					: this.properties.getPassword();
			config.setUsername(username);
			if (password != null) {
				config.setPassword(RedisPassword.of(password));
			}
			String sentinelUsername = (this.serviceConnection != null)
					? this.serviceConnection.getSentinel().getUsername() : sentinelProperties.getUsername();
			config.setSentinelUsername(sentinelUsername);
			String sentinelPassword = (this.serviceConnection != null)
					? this.serviceConnection.getSentinel().getPassword() : sentinelProperties.getPassword();
			if (sentinelPassword != null) {
				config.setSentinelPassword(RedisPassword.of(sentinelPassword));
			}
			int database = (this.serviceConnection != null) ? this.serviceConnection.getSentinel().getDatabase()
					: this.properties.getDatabase();
			config.setDatabase(database);
			return config;
		}
		return null;
	}

	/**
	 * Create a {@link RedisClusterConfiguration} if necessary.
	 * @return {@literal null} if no cluster settings are set.
	 */
	protected final RedisClusterConfiguration getClusterConfiguration() {
		if (this.clusterConfiguration != null) {
			return this.clusterConfiguration;
		}
		RedisProperties.Cluster clusterProperties = this.properties.getCluster();
		if (clusterProperties != null
				|| (this.serviceConnection != null && this.serviceConnection.getCluster() != null)) {
			List<String> nodes = (this.serviceConnection != null) ? getNodes(this.serviceConnection.getCluster())
					: clusterProperties.getNodes();
			RedisClusterConfiguration config = new RedisClusterConfiguration(nodes);
			if (clusterProperties != null && clusterProperties.getMaxRedirects() != null) {
				config.setMaxRedirects(clusterProperties.getMaxRedirects());
			}
			String username = (this.serviceConnection != null) ? this.serviceConnection.getUsername()
					: this.properties.getUsername();
			String password = (this.serviceConnection != null) ? this.serviceConnection.getPassword()
					: this.properties.getPassword();
			config.setUsername(username);
			if (password != null) {
				config.setPassword(RedisPassword.of(password));
			}
			return config;
		}
		return null;
	}

	private List<String> getNodes(Cluster cluster) {
		return cluster.getNodes().stream().map((node) -> "%s:%d".formatted(node.host(), node.port())).toList();
	}

	protected final RedisProperties getProperties() {
		return this.properties;
	}

	protected boolean isPoolEnabled(Pool pool) {
		Boolean enabled = pool.getEnabled();
		return (enabled != null) ? enabled : COMMONS_POOL2_AVAILABLE;
	}

	private List<RedisNode> createSentinels(RedisProperties.Sentinel sentinel) {
		List<RedisNode> nodes = new ArrayList<>();
		for (String node : sentinel.getNodes()) {
			try {
				nodes.add(RedisNode.fromString(node));
			}
			catch (RuntimeException ex) {
				throw new IllegalStateException("Invalid redis sentinel property '" + node + "'", ex);
			}
		}
		return nodes;
	}

	private List<RedisNode> createSentinels(Sentinel sentinel) {
		List<RedisNode> nodes = new ArrayList<>();
		for (Node node : sentinel.getNodes()) {
			nodes.add(new RedisNode(node.host(), node.port()));
		}
		return nodes;
	}

	protected ConnectionInfo parseUrl(String url) {
		try {
			URI uri = new URI(url);
			String scheme = uri.getScheme();
			if (!"redis".equals(scheme) && !"rediss".equals(scheme)) {
				throw new RedisUrlSyntaxException(url);
			}
			boolean useSsl = ("rediss".equals(scheme));
			String username = null;
			String password = null;
			if (uri.getUserInfo() != null) {
				String candidate = uri.getUserInfo();
				int index = candidate.indexOf(':');
				if (index >= 0) {
					username = candidate.substring(0, index);
					password = candidate.substring(index + 1);
				}
				else {
					password = candidate;
				}
			}
			return new ConnectionInfo(uri, useSsl, username, password);
		}
		catch (URISyntaxException ex) {
			throw new RedisUrlSyntaxException(url, ex);
		}
	}

	static class ConnectionInfo {

		private final URI uri;

		private final boolean useSsl;

		private final String username;

		private final String password;

		ConnectionInfo(URI uri, boolean useSsl, String username, String password) {
			this.uri = uri;
			this.useSsl = useSsl;
			this.username = username;
			this.password = password;
		}

		boolean isUseSsl() {
			return this.useSsl;
		}

		String getHostName() {
			return this.uri.getHost();
		}

		int getPort() {
			return this.uri.getPort();
		}

		String getUsername() {
			return this.username;
		}

		String getPassword() {
			return this.password;
		}

	}

}
