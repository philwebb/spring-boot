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

package org.springframework.boot.autoconfigure.cassandra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.ReconnectionPolicy;
import com.datastax.driver.core.policies.RetryPolicy;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Cassandra.
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ Cluster.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	private static final Log logger = LogFactory.getLog(CassandraAutoConfiguration.class);

	@Autowired
	private CassandraProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public Cluster cluster() {
		Cluster.Builder builder = Cluster.builder()
				.withClusterName(this.properties.getClusterName())
				.withPort(this.properties.getPort());
		builder.withCompression(getCompression(this.properties.getCompression()));

		// Manage the load balancing policy
		if (!StringUtils.isEmpty(this.properties.getLoadBalancingPolicy())) {
			try {
				Class loadBalancingPolicyClass = ClassUtils.forName(
						this.properties.getLoadBalancingPolicy(), null);
				Object loadBalancingPolicyInstance = loadBalancingPolicyClass
						.newInstance();
				LoadBalancingPolicy userLoadBalancingPolicy = (LoadBalancingPolicy) loadBalancingPolicyInstance;
				builder.withLoadBalancingPolicy(userLoadBalancingPolicy);
			}
			catch (ClassNotFoundException e) {
				logger.warn(
						"The load balancing policy could not be loaded, falling back to the default policy",
						e);
			}
			catch (InstantiationException e) {
				logger.warn(
						"The load balancing policy could not be instanced, falling back to the default policy",
						e);
			}
			catch (IllegalAccessException e) {
				logger.warn(
						"The load balancing policy could not be created, falling back to the default policy",
						e);
			}
			catch (ClassCastException e) {
				logger.warn(
						"The load balancing policy does not implement the correct interface, falling back to the default policy",
						e);
			}
		}

		// Manage query options
		QueryOptions queryOptions = new QueryOptions();
		if (this.properties.getConsistency() != null) {
			ConsistencyLevel consistencyLevel = ConsistencyLevel.valueOf(this.properties
					.getConsistency());
			queryOptions.setConsistencyLevel(consistencyLevel);
		}
		if (this.properties.getSerialConsistency() != null) {
			ConsistencyLevel serialConsistencyLevel = ConsistencyLevel
					.valueOf(this.properties.getSerialConsistency());
			queryOptions.setSerialConsistencyLevel(serialConsistencyLevel);
		}
		queryOptions.setFetchSize(this.properties.getFetchSize());
		builder.withQueryOptions(queryOptions);

		// Manage the reconnection policy
		if (!StringUtils.isEmpty(this.properties.getReconnectionPolicy())) {
			try {
				Class reconnectionPolicyClass = ClassUtils.forName(
						this.properties.getReconnectionPolicy(), null);
				Object reconnectionPolicyInstance = reconnectionPolicyClass.newInstance();
				ReconnectionPolicy userReconnectionPolicy = (ReconnectionPolicy) reconnectionPolicyInstance;
				builder.withReconnectionPolicy(userReconnectionPolicy);
			}
			catch (ClassNotFoundException e) {
				logger.warn(
						"The reconnection policy could not be loaded, falling back to the default policy",
						e);
			}
			catch (InstantiationException e) {
				logger.warn(
						"The reconnection policy could not be instanced, falling back to the default policy",
						e);
			}
			catch (IllegalAccessException e) {
				logger.warn(
						"The reconnection policy could not be created, falling back to the default policy",
						e);
			}
			catch (ClassCastException e) {
				logger.warn(
						"The reconnection policy does not implement the correct interface, falling back to the default policy",
						e);
			}
		}

		// Manage the retry policy
		if (!StringUtils.isEmpty(this.properties.getRetryPolicy())) {
			try {
				Class retryPolicyClass = ClassUtils.forName(
						this.properties.getRetryPolicy(), null);
				Object retryPolicyInstance = retryPolicyClass.newInstance();
				RetryPolicy userRetryPolicy = (RetryPolicy) retryPolicyInstance;
				builder.withRetryPolicy(userRetryPolicy);
			}
			catch (ClassNotFoundException e) {
				logger.warn(
						"The retry policy could not be loaded, falling back to the default policy",
						e);
			}
			catch (InstantiationException e) {
				logger.warn(
						"The retry policy could not be instanced, falling back to the default policy",
						e);
			}
			catch (IllegalAccessException e) {
				logger.warn(
						"The retry policy could not be created, falling back to the default policy",
						e);
			}
			catch (ClassCastException e) {
				logger.warn(
						"The retry policy does not implement the correct interface, falling back to the default policy",
						e);
			}
		}

		// Manage socket options
		SocketOptions socketOptions = new SocketOptions();
		socketOptions.setConnectTimeoutMillis(this.properties.getConnectTimeoutMillis());
		socketOptions.setReadTimeoutMillis(this.properties.getReadTimeoutMillis());
		builder.withSocketOptions(socketOptions);

		// Manage SSL
		if (this.properties.isSsl()) {
			builder.withSSL();
		}

		// Manage the contact points
		builder.addContactPoints(StringUtils
				.commaDelimitedListToStringArray(this.properties.getContactPoints()));

		return builder.build();
	}

	private Compression getCompression(CassandraProperties.Compression compression) {
		if (compression == null) {
			return Compression.NONE;
		}
		return Compression.valueOf(compression.name());
	}

}
