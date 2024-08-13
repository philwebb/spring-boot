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

import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionConfiguration.ConnectionInfo;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
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
		if (StringUtils.hasLength(this.properties.getSsl().getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(this.properties.getSsl().getBundle());
		}
		// TODO MH: Cassandra has the same thing, refactor this
		// SSL is enabled, but no bundle has been set -> use the default SSLContext
		return SslBundle.of(null, null, null, null, new SslManagerBundle() {
			@Override
			public KeyManagerFactory getKeyManagerFactory() {
				// TODO MH: Not sure this is the correct way to do things
				try {
					return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				}
				catch (NoSuchAlgorithmException ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public TrustManagerFactory getTrustManagerFactory() {
				// TODO MH: Not sure this is the correct way to do things
				try {
					return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				}
				catch (NoSuchAlgorithmException ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			public SSLContext createSslContext(String protocol) {
				try {
					return SSLContext.getDefault();
				}
				catch (NoSuchAlgorithmException ex) {
					throw new IllegalStateException(ex);
				}
			}
		});
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
		List<Node> nodes = (cluster != null) ? cluster.getNodes().stream().map(this::asNode).toList() : null;
		if (nodes == null) {
			return null;
		}
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
