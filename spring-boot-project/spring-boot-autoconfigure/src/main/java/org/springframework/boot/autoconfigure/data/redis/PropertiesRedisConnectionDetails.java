/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.List;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionConfiguration.ConnectionInfo;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link RedisProperties} to {@link RedisConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PropertiesRedisConnectionDetails implements RedisConnectionDetails {

	private final RedisProperties properties;

	private final SslBundles sslBundles;

	PropertiesRedisConnectionDetails(RedisProperties properties, SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public String getUsername() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return connectionInfo.getUsername();
		}
		return this.properties.getUsername();
	}

	@Override
	public String getPassword() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return connectionInfo.getPassword();
		}
		return this.properties.getPassword();
	}

	@Override
	public Standalone getStandalone() {
		if (this.properties.getUrl() != null) {
			ConnectionInfo connectionInfo = connectionInfo(this.properties.getUrl());
			return Standalone.of(connectionInfo.getUri().getHost(), connectionInfo.getUri().getPort(),
					this.properties.getDatabase(), getSslBundle());
		}
		return Standalone.of(this.properties.getHost(), this.properties.getPort(), this.properties.getDatabase(),
				getSslBundle());
	}

	private SslBundle getSslBundle() {
		if (!this.properties.getSsl().isEnabled()) {
			return null;
		}
		String bundleName = this.properties.getSsl().getBundle();
		if (StringUtils.hasLength(bundleName)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundleName);
		}
		return SslBundle.systemDefault();
	}

	private ConnectionInfo connectionInfo(String url) {
		return (url != null) ? RedisConnectionConfiguration.parseUrl(url) : null;
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

			@Override
			public SslBundle getSslBundle() {
				return PropertiesRedisConnectionDetails.this.getSslBundle();
			}
		};
	}

	@Override
	public Cluster getCluster() {
		RedisProperties.Cluster cluster = this.properties.getCluster();
		if (cluster == null) {
			return null;
		}
		List<Node> nodes = cluster.getNodes().stream().map(this::asNode).toList();
		return new Cluster() {
			@Override
			public List<Node> getNodes() {
				return nodes;
			}

			@Override
			public SslBundle getSslBundle() {
				return PropertiesRedisConnectionDetails.this.getSslBundle();
			}
		};
	}

	private Node asNode(String node) {
		int portSeparatorIndex = node.lastIndexOf(':');
		String host = node.substring(0, portSeparatorIndex);
		int port = Integer.parseInt(node.substring(portSeparatorIndex + 1));
		return new Node(host, port);
	}

}
