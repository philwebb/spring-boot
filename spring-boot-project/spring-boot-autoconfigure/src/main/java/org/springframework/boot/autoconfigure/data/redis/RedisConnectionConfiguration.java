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
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Cluster;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Node;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Sentinel;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.util.ClassUtils;

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

	protected final RedisConnectionDetails connectionDetails;

	protected RedisConnectionConfiguration(RedisProperties properties,
			ObjectProvider<RedisStandaloneConfiguration> standaloneConfigurationProvider,
			ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
			ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
			ObjectProvider<RedisConnectionDetails> connectionDetailsProvider) {
		this.properties = properties;
		this.standaloneConfiguration = standaloneConfigurationProvider.getIfAvailable();
		this.sentinelConfiguration = sentinelConfigurationProvider.getIfAvailable();
		this.clusterConfiguration = clusterConfigurationProvider.getIfAvailable();
		this.connectionDetails = connectionDetailsProvider
			.getIfAvailable(() -> new PropertiesRedisConnectionDetails(properties));
	}

	protected final RedisStandaloneConfiguration getStandaloneConfig() {
		if (this.standaloneConfiguration != null) {
			return this.standaloneConfiguration;
		}
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
		config.setHostName(this.connectionDetails.getStandalone().getHost());
		config.setPort(this.connectionDetails.getStandalone().getPort());
		config.setUsername(this.connectionDetails.getUsername());
		config.setPassword(RedisPassword.of(this.connectionDetails.getPassword()));
		config.setDatabase(this.connectionDetails.getStandalone().getDatabase());
		return config;
	}

	protected final RedisSentinelConfiguration getSentinelConfig() {
		if (this.sentinelConfiguration != null) {
			return this.sentinelConfiguration;
		}
		if (this.connectionDetails.getSentinel() != null) {
			RedisSentinelConfiguration config = new RedisSentinelConfiguration();
			config.master(this.connectionDetails.getSentinel().getMaster());
			config.setSentinels(createSentinels(this.connectionDetails.getSentinel()));
			config.setUsername(this.connectionDetails.getUsername());
			String password = this.connectionDetails.getPassword();
			if (password != null) {
				config.setPassword(RedisPassword.of(password));
			}
			config.setSentinelUsername(this.connectionDetails.getSentinel().getUsername());
			String sentinelPassword = this.connectionDetails.getSentinel().getPassword();
			if (sentinelPassword != null) {
				config.setSentinelPassword(RedisPassword.of(sentinelPassword));
			}
			config.setDatabase(this.connectionDetails.getSentinel().getDatabase());
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
		if (this.connectionDetails.getCluster() != null) {
			RedisClusterConfiguration config = new RedisClusterConfiguration(
					getNodes(this.connectionDetails.getCluster()));
			if (clusterProperties != null && clusterProperties.getMaxRedirects() != null) {
				config.setMaxRedirects(clusterProperties.getMaxRedirects());
			}
			config.setUsername(this.connectionDetails.getUsername());
			String password = this.connectionDetails.getPassword();
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

	private List<RedisNode> createSentinels(Sentinel sentinel) {
		List<RedisNode> nodes = new ArrayList<>();
		for (Node node : sentinel.getNodes()) {
			nodes.add(new RedisNode(node.host(), node.port()));
		}
		return nodes;
	}

	protected boolean useSsl() {
		return parseUrl(this.properties.getUrl()).isUseSsl();
	}

	private static ConnectionInfo parseUrl(String url) {
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

	static class PropertiesRedisConnectionDetails implements RedisConnectionDetails {

		private final RedisProperties properties;

		PropertiesRedisConnectionDetails(RedisProperties properties) {
			this.properties = properties;
		}

		@Override
		public String getUsername() {
			if (this.properties.getUrl() != null) {
				ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
				String userInfo = connectionInfo.uri.getUserInfo();
				if (userInfo != null) {
					int index = userInfo.indexOf(':');
					if (index != -1) {
						return userInfo.substring(0, index);
					}
				}
			}
			return this.properties.getUsername();
		}

		@Override
		public String getPassword() {
			if (this.properties.getUrl() != null) {
				ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
				String userInfo = connectionInfo.uri.getUserInfo();
				if (userInfo != null) {
					int index = userInfo.indexOf(':');
					if (index != -1) {
						return userInfo.substring(index + 1);
					}
				}
			}
			return this.properties.getPassword();
		}

		@Override
		public Standalone getStandalone() {
			if (this.properties.getUrl() != null) {
				ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
				return Standalone.of(connectionInfo.getHostName(), connectionInfo.getPort(),
						this.properties.getDatabase());
			}
			return Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase());
		}

		private ConnectionInfo connectionInfo(String url) {
			if (url == null) {
				return null;
			}
			return parseUrl(url);
		}

		@Override
		public Sentinel getSentinel() {
			org.springframework.boot.autoconfigure.data.redis.RedisProperties.Sentinel sentinel = this.properties
				.getSentinel();
			if (sentinel == null) {
				return null;
			}
			return new Sentinel() {

				@Override
				public int getDatabase() {
					return PropertiesRedisConnectionDetails.this.properties.getDatabase();
				}

				@Override
				public String getMaster() {
					return sentinel.getMaster();
				}

				@Override
				public List<Node> getNodes() {
					return sentinel.getNodes().stream().map(PropertiesRedisConnectionDetails.this::asNode).toList();
				}

				@Override
				public String getUsername() {
					return sentinel.getUsername();
				}

				@Override
				public String getPassword() {
					return sentinel.getPassword();
				}

			};
		}

		@Override
		public Cluster getCluster() {
			org.springframework.boot.autoconfigure.data.redis.RedisProperties.Cluster cluster = this.properties
				.getCluster();
			if (cluster == null) {
				return null;
			}
			return new Cluster() {

				@Override
				public List<Node> getNodes() {
					return cluster.getNodes().stream().map(PropertiesRedisConnectionDetails.this::asNode).toList();
				}

			};
		}

		private Node asNode(String node) {
			String[] components = node.split(":");
			return new Node(components[0], Integer.parseInt(components[1]));
		}

	}

}
