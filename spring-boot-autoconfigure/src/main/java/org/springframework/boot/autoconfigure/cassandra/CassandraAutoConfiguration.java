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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;

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
		if (this.properties.getLoadBalancingPolicy() != null) {
			builder.withLoadBalancingPolicy(BeanUtils.instantiate(this.properties
					.getLoadBalancingPolicy()));
		}
		builder.withQueryOptions(getQueryOptions());
		if (this.properties.getReconnectionPolicy() != null) {
			builder.withReconnectionPolicy(BeanUtils.instantiate(this.properties
					.getReconnectionPolicy()));
		}
		if (this.properties.getRetryPolicy() != null) {
			builder.withRetryPolicy(BeanUtils.instantiate(this.properties
					.getRetryPolicy()));
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

	private QueryOptions getQueryOptions() {
		QueryOptions options = new QueryOptions();
		if (this.properties.getConsistencyLevel() != null) {
			options.setConsistencyLevel(ConsistencyLevel.valueOf(this.properties
					.getConsistencyLevel().name()));
		}
		if (this.properties.getSerialConsistencyLevel() != null) {
			options.setSerialConsistencyLevel(ConsistencyLevel.valueOf(this.properties
					.getSerialConsistencyLevel().name()));
		}
		options.setFetchSize(this.properties.getFetchSize());
		return options;
	}

}
