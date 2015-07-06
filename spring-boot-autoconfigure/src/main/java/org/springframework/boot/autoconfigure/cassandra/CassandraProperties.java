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
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;

/**
 * Configuration properties for Cassandra.
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.data.cassandra")
public class CassandraProperties {

	private static final Log logger = LogFactory.getLog(CassandraProperties.class);

	/**
	 * Name of the Cassandra cluster.
	 */
	private String clusterName = "Test Cluster";

	private int port = ProtocolOptions.DEFAULT_PORT;

	/**
	 * Comma-separated list of cluster node addresses.
	 */
	private String contactPoints = "localhost";

	/**
	 * Compression supported by the Cassandra binary protocol: can be NONE, SNAPPY, LZ4.
	 */
	private String compression = ProtocolOptions.Compression.NONE.name();

	/**
	 * Class name of the load balancing policy.
	 */
	private String loadBalancingPolicy;

	/**
	 * Queries consistency level.
	 */
	private String consistency;

	/**
	 * Queries serial consistency level.
	 */
	private String serialConsistency;

	/**
	 * Queries default fetch size.
	 */
	private int fetchSize = QueryOptions.DEFAULT_FETCH_SIZE;

	/**
	 * Class name of the reconnection policy.
	 */
	private String reconnectionPolicy;

	/**
	 * Class name of the retry policy.
	 */
	private String retryPolicy;

	/**
	 * Socket option: connection time out.
	 */
	private int connectTimeoutMillis = SocketOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS;

	/**
	 * Socket option: read time out.
	 */
	private int readTimeoutMillis = SocketOptions.DEFAULT_READ_TIMEOUT_MILLIS;

	/**
	 * Enable SSL support.
	 */
	private boolean ssl = false;

	/**
	 * Keyspace name to use.
	 */
	private String keyspaceName;

	public String getClusterName() {
		return this.clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getContactPoints() {
		return this.contactPoints;
	}

	public void setContactPoints(String contactPoints) {
		this.contactPoints = contactPoints;
	}

	public String getCompression() {
		return this.compression;
	}

	public void setCompression(String compression) {
		this.compression = compression;
	}

	public String getLoadBalancingPolicy() {
		return this.loadBalancingPolicy;
	}

	public void setLoadBalancingPolicy(String loadBalancingPolicy) {
		this.loadBalancingPolicy = loadBalancingPolicy;
	}

	public String getConsistency() {
		return this.consistency;
	}

	public void setConsistency(String consistency) {
		this.consistency = consistency;
	}

	public String getSerialConsistency() {
		return this.serialConsistency;
	}

	public void setSerialConsistency(String serialConsistency) {
		this.serialConsistency = serialConsistency;
	}

	public int getFetchSize() {
		return this.fetchSize;
	}

	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public String getReconnectionPolicy() {
		return this.reconnectionPolicy;
	}

	public void setReconnectionPolicy(String reconnectionPolicy) {
		this.reconnectionPolicy = reconnectionPolicy;
	}

	public String getRetryPolicy() {
		return this.retryPolicy;
	}

	public void setRetryPolicy(String retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	public int getConnectTimeoutMillis() {
		return this.connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getReadTimeoutMillis() {
		return this.readTimeoutMillis;
	}

	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public boolean isSsl() {
		return this.ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getKeyspaceName() {
		return this.keyspaceName;
	}

	public void setKeyspaceName(String keyspaceName) {
		this.keyspaceName = keyspaceName;
	}

}
