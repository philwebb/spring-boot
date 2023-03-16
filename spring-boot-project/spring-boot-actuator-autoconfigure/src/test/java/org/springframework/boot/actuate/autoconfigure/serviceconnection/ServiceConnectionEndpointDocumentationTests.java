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
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation.MockMvcEndpointDocumentationTests;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinServiceConnection;
import org.springframework.boot.autoconfigure.amqp.RabbitServiceConnection;
import org.springframework.boot.autoconfigure.data.redis.RedisServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node.Protocol;
import org.springframework.boot.autoconfigure.jdbc.JdbcServiceConnection;
import org.springframework.boot.autoconfigure.mongo.MongoServiceConnection;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcServiceConnection;
import org.springframework.boot.autoconfigure.serviceconnection.ServiceConnection;
import org.springframework.boot.origin.Origin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link ServiceConnectionEndpoint}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ServiceConnectionEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	@Test
	void serviceconnection() throws Exception {
		this.mockMvc.perform(get("/actuator/serviceconnection"))
			.andExpect(status().isOk())
			.andDo(MockMvcRestDocumentation.document("serviceconnection",
					responseFields(fieldWithPath("jdbc").description("JDBC service connections"))
						.andWithPrefix("jdbc.[].", jdbcFields())
						.and(fieldWithPath("r2dbc").description("R2DBC service connections"))
						.andWithPrefix("r2dbc.[].", r2dbcFields())
						.and(fieldWithPath("elasticsearch").description("Elasticsearch service connections"))
						.andWithPrefix("elasticsearch.[].", elasticsearchFields())
						.and(fieldWithPath("mongo").description("MongoDB service connections"))
						.andWithPrefix("mongo.[].", mongoFields())
						.and(fieldWithPath("rabbit").description("RabbitMQ service connections"))
						.andWithPrefix("rabbit.[].", rabbitFields())
						.and(fieldWithPath("redis").description("Redis service connections"))
						.andWithPrefix("redis.[].", redisFields())
						.and(fieldWithPath("zipkin").description("Zipkin service connections"))
						.andWithPrefix("zipkin.[].", zipkinFields())));
	}

	private List<FieldDescriptor> zipkinFields() {
		return List.of(fieldWithPath("name").description("Name of the Zipkin service connection"),
				fieldWithPath("origin").description("Origin of the Zipkin service connection").optional(),
				fieldWithPath("host").description("Hostname of the Zipkin service"),
				fieldWithPath("port").description("Port of the Zipkin service"),
				fieldWithPath("spanPath").description("Path of the span reporting endpoint of the Zipkin service"));
	}

	private List<FieldDescriptor> rabbitFields() {
		return List.of(fieldWithPath("name").description("Name of the RabbitMQ service connection"),
				fieldWithPath("origin").description("Origin of the RabbitMQ service connection").optional(),
				fieldWithPath("username").description("Login user to authenticate to the broker").optional(),
				fieldWithPath("virtualHost").description("Virtual host to use when connecting to the broker")
					.optional(),
				fieldWithPath("addresses").description("List of addresses to which the client should connect"),
				fieldWithPath("addresses.[].host").description("Hostname of the RabbitMQ server"),
				fieldWithPath("addresses.[].port").description("Port of the RabbitMQ server"));
	}

	private List<FieldDescriptor> redisFields() {
		return List.of(fieldWithPath("name").description("Name of the Redis service connection"),
				fieldWithPath("origin").description("Origin of the Redis service connection").optional(),
				fieldWithPath("username").description("Login username of the redis server").optional(),
				fieldWithPath("standalone").description("Redis standalone configuration").optional(),
				fieldWithPath("standalone.host").description("Redis server host"),
				fieldWithPath("standalone.port").description("Redis server port"),
				fieldWithPath("standalone.database").description("Database index used by the connection factory"),
				fieldWithPath("sentinel").description("Redis sentinel configuration").optional(),
				fieldWithPath("sentinel.database").description("Database index used by the connection factory"),
				fieldWithPath("sentinel.master").description("Name of the Redis server"),
				fieldWithPath("sentinel.username").description("Login username for authenticating with sentinel(s)")
					.optional(),
				fieldWithPath("sentinel.nodes").description("List of nodes"),
				fieldWithPath("sentinel.nodes.[].host").description("Hostname of the Redis sentinel"),
				fieldWithPath("sentinel.nodes.[].port").description("Port of the Redis sentinel"),
				fieldWithPath("cluster").description("Redis cluster configuration").optional(),
				fieldWithPath("cluster.nodes").description("Nodes to bootstrap from"),
				fieldWithPath("cluster.nodes.[].host").description("Hostname of the Redis cluster node"),
				fieldWithPath("cluster.nodes.[].port").description("Port of the Redis cluster node"));
	}

	private List<FieldDescriptor> mongoFields() {
		return List.of(fieldWithPath("name").description("Name of the Mongo service connection"),
				fieldWithPath("origin").description("Origin of the Mongo service connection").optional(),
				fieldWithPath("host").description("Mongo server host"),
				fieldWithPath("port").description("Mongo server port"),
				fieldWithPath("additionalHosts").description("Additional server hosts"),
				fieldWithPath("additionalHosts.[].host").description("Hostname of the MongoDB instance"),
				fieldWithPath("additionalHosts.[].port").description("Port of the MongoDB instance"),
				fieldWithPath("database").description("Database name"),
				fieldWithPath("authenticationDatabase").description("Authentication database name.").optional(),
				fieldWithPath("username").description("Login user of the mongo server").optional(),
				fieldWithPath("replicaSetName").description("Replica set name for the cluster."),
				fieldWithPath("gridFs").description("GridFS configuration").optional(),
				fieldWithPath("gridFs.database").description("GridFS database name"));
	}

	private List<FieldDescriptor> jdbcFields() {
		return List.of(fieldWithPath("name").description("Name of the JDBC service connection"),
				fieldWithPath("origin").description("Origin of the JDBC service connection").optional(),
				fieldWithPath("url").description("URL of the JDBC service connection"),
				fieldWithPath("username").description("Username of the JDBC service connection").optional());
	}

	private List<FieldDescriptor> r2dbcFields() {
		return List.of(fieldWithPath("name").description("Name of the R2DBC service connection"),
				fieldWithPath("origin").description("Origin of the R2DBC service connection").optional(),
				fieldWithPath("url").description("URL of the R2DBC service connection"),
				fieldWithPath("username").description("Username of the R2DBC service connection").optional());
	}

	private List<FieldDescriptor> elasticsearchFields() {
		return List.of(fieldWithPath("name").description("Name of the Elasticsearch service connection"),
				fieldWithPath("origin").description("Origin of the Elasticsearch service connection").optional(),
				fieldWithPath("pathPrefix")
					.description("Prefix added to the path of every request sent to Elasticsearch")
					.optional(),
				fieldWithPath("username").description("Username for authentication with Elasticsearch").optional(),
				fieldWithPath("nodes").description("List of Elasticsearch nodes to use"),
				fieldWithPath("nodes.[].host").description("Host of the Elasticsearch node"),
				fieldWithPath("nodes.[].port").description("Port of the Elasticsearch node"),
				fieldWithPath("nodes.[].protocol").description("Protocol of the Elasticsearch node"),
				fieldWithPath("nodes.[].username").description("Username of the Elasticsearch node").optional());
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		ServiceConnectionEndpoint endpoint(ObjectProvider<ServiceConnection> serviceConnectionProvider) {
			return new ServiceConnectionEndpoint(serviceConnectionProvider);
		}

		@Bean
		RedisServiceConnection redisServiceConnection() {
			return new RedisServiceConnection() {
				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

				@Override
				public Standalone getStandalone() {
					return new Standalone() {
						@Override
						public int getDatabase() {
							return 0;
						}

						@Override
						public String getHost() {
							return "localhost";
						}

						@Override
						public int getPort() {
							return 6379;
						}
					};
				}

				@Override
				public Sentinel getSentinel() {
					return new Sentinel() {
						@Override
						public int getDatabase() {
							return 0;
						}

						@Override
						public String getMaster() {
							return "localhost";
						}

						@Override
						public List<Node> getNodes() {
							return List.of(new Node("localhost", 23456));
						}

						@Override
						public String getUsername() {
							return "user-1";
						}

						@Override
						public String getPassword() {
							return "password-1";
						}
					};
				}

				@Override
				public Cluster getCluster() {
					return new Cluster() {
						@Override
						public List<Node> getNodes() {
							return List.of(new Node("localhost", 12345));
						}
					};
				}

				@Override
				public String getName() {
					return "redisServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method redisServiceConnection");
				}
			};
		}

		@Bean
		ZipkinServiceConnection zipkinServiceConnection() {
			return new ZipkinServiceConnection() {
				@Override
				public String getHost() {
					return "localhost";
				}

				@Override
				public int getPort() {
					return 9411;
				}

				@Override
				public String getSpanPath() {
					return "/api/v2/span";
				}

				@Override
				public String getName() {
					return "zipkinServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method zipkinServiceConnection");
				}
			};
		}

		@Bean
		JdbcServiceConnection jdbcServiceConnection() {
			return new JdbcServiceConnection() {
				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

				@Override
				public String getJdbcUrl() {
					return "jdbc:mysql:localhost/database-1";
				}

				@Override
				public String getName() {
					return "jdbcServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method jdbcServiceConnection");
				}
			};
		}

		@Bean
		R2dbcServiceConnection r2dbcServiceConnection() {
			return new R2dbcServiceConnection() {
				@Override
				public String getUsername() {
					return "user-2";
				}

				@Override
				public String getPassword() {
					return "secret-2";
				}

				@Override
				public String getR2dbcUrl() {
					return "jdbc:postgresql:localhost/database-1";
				}

				@Override
				public String getName() {
					return "r2dbcServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method r2dbcServiceConnection");
				}
			};
		}

		@Bean
		RabbitServiceConnection rabbitServiceConnection() {
			return new RabbitServiceConnection() {
				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public String getVirtualHost() {
					return "vhost-1";
				}

				@Override
				public List<Address> getAddresses() {
					return List.of(new Address("localhost", 12345));
				}

				@Override
				public String getName() {
					return "rabbitServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method rabbitServiceConnection");
				}
			};
		}

		@Bean
		ElasticsearchServiceConnection elasticsearchServiceConnection() {
			return new ElasticsearchServiceConnection() {
				@Override
				public List<Node> getNodes() {
					return List.of(new Node("localhost", 12345, Protocol.HTTP, "user", "password"));
				}

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

				@Override
				public String getPathPrefix() {
					return "/";
				}

				@Override
				public String getName() {
					return "elasticsearchServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method elasticsearchServiceConnection");
				}
			};
		}

		@Bean
		MongoServiceConnection mongoServiceConnection() {
			return new MongoServiceConnection() {
				@Override
				public String getHost() {
					return "localhost";
				}

				@Override
				public int getPort() {
					return 12345;
				}

				@Override
				public List<Host> getAdditionalHosts() {
					return List.of(new Host("localhost", 23456));
				}

				@Override
				public String getDatabase() {
					return "database-1";
				}

				@Override
				public String getAuthenticationDatabase() {
					return "admin";
				}

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

				@Override
				public String getReplicaSetName() {
					return "replica-1";
				}

				@Override
				public GridFs getGridFs() {
					return new GridFs() {
						@Override
						public String getDatabase() {
							return "grid-database-1";
						}
					};
				}

				@Override
				public String getName() {
					return "mongoServiceConnection";
				}

				@Override
				public Origin getOrigin() {
					return new DummyOrigin("bean method mongoServiceConnection");
				}
			};
		}

	}

	private static class DummyOrigin implements Origin {

		private final String text;

		DummyOrigin(String text) {
			this.text = text;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			DummyOrigin that = (DummyOrigin) o;
			return Objects.equals(this.text, that.text);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.text);
		}

		@Override
		public String toString() {
			return this.text;
		}

	}

}
