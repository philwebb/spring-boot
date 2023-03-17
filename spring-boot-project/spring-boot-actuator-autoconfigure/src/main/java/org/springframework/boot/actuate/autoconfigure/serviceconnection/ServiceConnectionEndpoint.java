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

package org.springframework.boot.actuate.autoconfigure.serviceconnection;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails.Address;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Cluster;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Sentinel;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Standalone;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails.GridFs;
import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails.Host;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.origin.Origin;

/**
 * {@link Endpoint @Endpoint} to expose service connections.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
@Endpoint(id = "serviceconnection")
class ServiceConnectionEndpoint {

	private final ObjectProvider<ConnectionDetails> serviceConnectionProvider;

	ServiceConnectionEndpoint(ObjectProvider<ConnectionDetails> serviceConnectionProvider) {
		this.serviceConnectionProvider = serviceConnectionProvider;
	}

	@ReadOperation
	ServiceConnectionsDto serviceConnections() {
		Stream<RedisConnectionDetails> redis = getServiceConnections(RedisConnectionDetails.class);
		Stream<ZipkinConnectionDetails> zipkin = getServiceConnections(ZipkinConnectionDetails.class);
		Stream<JdbcConnectionDetails> jdbc = getServiceConnections(JdbcConnectionDetails.class);
		Stream<R2dbcConnectionDetails> r2dbc = getServiceConnections(R2dbcConnectionDetails.class);
		Stream<RabbitConnectionDetails> rabbit = getServiceConnections(RabbitConnectionDetails.class);
		Stream<ElasticsearchConnectionDetails> elasticsearch = getServiceConnections(
				ElasticsearchConnectionDetails.class);
		Stream<MongoConnectionDetails> mongo = getServiceConnections(MongoConnectionDetails.class);
		return new ServiceConnectionsDto(redis.map(RedisServiceConnectionDto::from).toList(),
				zipkin.map(ZipkinServiceConnectionDto::from).toList(),
				jdbc.map(JdbcServiceConnectionDto::from).toList(), r2dbc.map(R2dbcServiceConnectionDto::from).toList(),
				rabbit.map(RabbitServiceConnectionDto::from).toList(),
				elasticsearch.map(ElasticsearchServiceConnectionDto::from).toList(),
				mongo.map(MongoServiceConnectionDto::from).toList());
	}

	@SuppressWarnings("unchecked")
	private <T> Stream<T> getServiceConnections(Class<T> serviceConnectionClass) {
		return (Stream<T>) this.serviceConnectionProvider.stream()
			.filter((serviceConnection) -> serviceConnectionClass.isAssignableFrom(serviceConnection.getClass()));
	}

	@JsonPropertyOrder(alphabetic = true)
	record ServiceConnectionsDto(List<RedisServiceConnectionDto> redis, List<ZipkinServiceConnectionDto> zipkin,
			List<JdbcServiceConnectionDto> jdbc, List<R2dbcServiceConnectionDto> r2dbc,
			List<RabbitServiceConnectionDto> rabbit, List<ElasticsearchServiceConnectionDto> elasticsearch,
			List<MongoServiceConnectionDto> mongo) implements OperationResponseBody {
	}

	@JsonInclude(Include.NON_NULL)
	private record JdbcServiceConnectionDto(String name, String origin, String username, String url) {

		private static JdbcServiceConnectionDto from(JdbcConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new JdbcServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getUsername(),
					serviceConnection.getJdbcUrl());
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record R2dbcServiceConnectionDto(String name, String origin, String username, String url) {

		private static R2dbcServiceConnectionDto from(R2dbcConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new R2dbcServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getUsername(),
					serviceConnection.getR2dbcUrl());
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record ZipkinServiceConnectionDto(String name, String origin, String host, int port, String spanPath) {

		private static ZipkinServiceConnectionDto from(ZipkinConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new ZipkinServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getHost(),
					serviceConnection.getPort(), serviceConnection.getSpanPath());
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record RedisServiceConnectionDto(String name, String origin, String username, StandaloneDto standalone,
			SentinelDto sentinel, ClusterDto cluster) {

		private static RedisServiceConnectionDto from(RedisConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new RedisServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getUsername(),
					StandaloneDto.from(serviceConnection.getStandalone()),
					SentinelDto.from(serviceConnection.getSentinel()), ClusterDto.from(serviceConnection.getCluster()));
		}

		private record StandaloneDto(String host, int port, int database) {
			static StandaloneDto from(Standalone standalone) {
				if (standalone == null) {
					return null;
				}
				return new StandaloneDto(standalone.getHost(), standalone.getPort(), standalone.getDatabase());
			}
		}

		private record SentinelDto(int database, String master, List<NodeDto> nodes, String username) {
			static SentinelDto from(Sentinel sentinel) {
				if (sentinel == null) {
					return null;
				}
				return new SentinelDto(sentinel.getDatabase(), sentinel.getMaster(),
						sentinel.getNodes().stream().map(NodeDto::from).toList(), sentinel.getUsername());
			}
		}

		private record ClusterDto(List<NodeDto> nodes) {
			static ClusterDto from(Cluster cluster) {
				if (cluster == null) {
					return null;
				}
				return new ClusterDto(cluster.getNodes().stream().map(NodeDto::from).toList());
			}
		}

		private record NodeDto(String host, int port) {
			private static NodeDto from(RedisConnectionDetails.Node node) {
				return new NodeDto(node.host(), node.port());
			}
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record RabbitServiceConnectionDto(String name, String origin, String username, String virtualHost,
			List<AddressDto> addresses) {

		private static RabbitServiceConnectionDto from(RabbitConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new RabbitServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getUsername(),
					serviceConnection.getVirtualHost(),
					serviceConnection.getAddresses().stream().map(AddressDto::from).toList());
		}

		private record AddressDto(String host, int port) {
			static AddressDto from(Address address) {
				return new AddressDto(address.host(), address.port());
			}
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record ElasticsearchServiceConnectionDto(String name, String origin, String username, String pathPrefix,
			List<NodeDto> nodes) {

		private static ElasticsearchServiceConnectionDto from(ElasticsearchConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new ElasticsearchServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getUsername(),
					serviceConnection.getPathPrefix(),
					serviceConnection.getNodes().stream().map(NodeDto::from).toList());
		}

		private record NodeDto(String host, int port, String protocol, String username) {
			static NodeDto from(Node node) {
				return new NodeDto(node.hostname(), node.port(), node.protocol().name(), node.username());
			}
		}
	}

	@JsonInclude(Include.NON_NULL)
	private record MongoServiceConnectionDto(String name, String origin, String host, int port,
			List<HostDto> additionalHosts, String database, String authenticationDatabase, String username,
			String replicaSetName, GridFsDto gridFs) {

		private static MongoServiceConnectionDto from(MongoConnectionDetails serviceConnection) {
			Origin origin = serviceConnection.getOrigin();
			return new MongoServiceConnectionDto(serviceConnection.getName(),
					(origin != null) ? origin.toString() : null, serviceConnection.getHost(),
					serviceConnection.getPort(),
					serviceConnection.getAdditionalHosts().stream().map(HostDto::from).toList(),
					serviceConnection.getDatabase(), serviceConnection.getAuthenticationDatabase(),
					serviceConnection.getUsername(), serviceConnection.getReplicaSetName(),
					GridFsDto.from(serviceConnection.getGridFs()));
		}

		private record HostDto(String host, int port) {
			private static HostDto from(Host host) {
				return new HostDto(host.host(), host.port());
			}
		}

		private record GridFsDto(String database) {
			private static GridFsDto from(GridFs gridFs) {
				if (gridFs == null) {
					return null;
				}
				return new GridFsDto(gridFs.getDatabase());
			}
		}
	}

}
