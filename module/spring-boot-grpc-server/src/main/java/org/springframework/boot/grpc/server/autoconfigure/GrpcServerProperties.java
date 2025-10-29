/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

@ConfigurationProperties(prefix = "spring.grpc.server")
public class GrpcServerProperties {

	/**
	 * The address to bind to in the form 'host:port' or a pseudo URL like
	 * 'static://host:port'.
	 */
	private @Nullable String address;

	private final Shutdown shutdown = new Shutdown();

	private final Inbound inbound = new Inbound();

	private final Health health = new Health();

	private final Inprocess inprocess = new Inprocess();

	private final Keepalive keepalive = new Keepalive();

	private final Ssl ssl = new Ssl();

	public @Nullable String getAddress() {
		return this.address;
	}

	public void setAddress(@Nullable String address) {
		this.address = address;
	}

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public Inbound getInbound() {
		return this.inbound;
	}

	public Health getHealth() {
		return this.health;
	}

	public Inprocess getInprocess() {
		return this.inprocess;
	}

	public Keepalive getKeepAlive() {
		return this.keepalive;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Shutdown {

		/**
		 * Maximum time to wait for the server to gracefully shutdown. When the value is
		 * negative, the server waits forever. When the value is 0, the server will force
		 * shutdown immediately. The default is 30 seconds.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration gracePeriod = Duration.ofSeconds(30);

		public Duration getGracePeriod() {
			return this.gracePeriod;
		}

		public void setGracePeriod(Duration gracePeriod) {
			this.gracePeriod = gracePeriod;
		}

	}

	public static class Inbound {

		private static final Message message = new Message();

		private static final Metadata metadata = new Metadata();

		public static Message getMessage() {
			return message;
		}

		public static Metadata getMetadata() {
			return metadata;
		}

		public static class Message {

			/**
			 * Maximum message size allowed to be received by the server (default 4MiB).
			 */
			@DataSizeUnit(DataUnit.BYTES)
			private DataSize maxSize = DataSize.ofBytes(4194304);

			public DataSize getMaxSize() {
				return this.maxSize;
			}

			public void setMaxSize(DataSize maxSize) {
				this.maxSize = maxSize;
			}

		}

		public static class Metadata {

			/**
			 * Maximum metadata size allowed to be received by the server (default 8KiB).
			 */
			@DataSizeUnit(DataUnit.BYTES)
			private DataSize maxSize = DataSize.ofBytes(8192);

			public DataSize getMaxSize() {
				return this.maxSize;
			}

			public void setMaxSize(DataSize maxSize) {
				this.maxSize = maxSize;
			}

		}

	}

	public static class Health {

		/**
		 * Whether to auto-configure Health feature on the gRPC server.
		 */
		private boolean enabled = true;

		private final Actuator actuator = new Actuator();

		public boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Actuator getActuator() {
			return this.actuator;
		}

		public static class Actuator {

			/**
			 * Whether to adapt Actuator health indicators into gRPC health checks.
			 */
			private boolean enabled = true;

			/**
			 * Whether to update the overall gRPC server health (the '' service) with the
			 * aggregate status of the configured health indicators.
			 */
			private boolean updateOverallHealth = true;

			/**
			 * How often to update the health status.
			 */
			private Duration updateRate = Duration.ofSeconds(5);

			/**
			 * The initial delay before updating the health status the very first time.
			 */
			private Duration updateInitialDelay = Duration.ofSeconds(5);

			/**
			 * List of Actuator health indicator paths to adapt into gRPC health checks.
			 */
			private List<String> healthIndicatorPaths = new ArrayList<>();

			public boolean getEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public boolean getUpdateOverallHealth() {
				return this.updateOverallHealth;
			}

			public void setUpdateOverallHealth(boolean updateOverallHealth) {
				this.updateOverallHealth = updateOverallHealth;
			}

			public Duration getUpdateRate() {
				return this.updateRate;
			}

			public void setUpdateRate(Duration updateRate) {
				this.updateRate = updateRate;
			}

			public Duration getUpdateInitialDelay() {
				return this.updateInitialDelay;
			}

			public void setUpdateInitialDelay(Duration updateInitialDelay) {
				this.updateInitialDelay = updateInitialDelay;
			}

			public List<String> getHealthIndicatorPaths() {
				return this.healthIndicatorPaths;
			}

			public void setHealthIndicatorPaths(List<String> healthIndicatorPaths) {
				this.healthIndicatorPaths = healthIndicatorPaths;
			}

		}

	}

	public static class Inprocess {

		/**
		 * The name of the in-process server or null to not start the in-process server.
		 */
		private @Nullable String name;

		public @Nullable String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

	}

	public static class Keepalive {

		/**
		 * Duration without read activity before sending a keep alive ping (default 2h).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration time = Duration.ofHours(2);

		/**
		 * Maximum time to wait for read activity after sending a keep alive ping. If
		 * sender does not receive an acknowledgment within this time, it will close the
		 * connection (default 20s).
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration timeout = Duration.ofSeconds(20);

		private final Permit permit = new Permit();

		private final Max max = new Max();

		public @Nullable Duration getTime() {
			return this.time;
		}

		public void setTime(@Nullable Duration time) {
			this.time = time;
		}

		public @Nullable Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(@Nullable Duration timeout) {
			this.timeout = timeout;
		}

		public Permit getPermit() {
			return this.permit;
		}

		public Max getMax() {
			return this.max;
		}

		public static class Permit {

			/**
			 * Maximum keep-alive time clients are permitted to configure (default 5m).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration time = Duration.ofMinutes(5);

			/**
			 * Whether clients are permitted to send keep alive pings when there are no
			 * outstanding RPCs on the connection (default false).
			 */
			private boolean withoutCalls;

			public @Nullable Duration getTime() {
				return this.time;
			}

			public void setTime(@Nullable Duration time) {
				this.time = time;
			}

			public boolean isWithoutCalls() {
				return this.withoutCalls;
			}

			public void setWithoutCalls(boolean withoutCalls) {
				this.withoutCalls = withoutCalls;
			}

		}

		public static class Max {

			/**
			 * Maximum time a connection can remain idle before being gracefully
			 * terminated (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration idle;

			/**
			 * Maximum time a connection may exist before being gracefully terminated
			 * (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration age;

			/**
			 * Maximum time for graceful connection termination (default infinite).
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private @Nullable Duration grace;

			// FIXME difference with this and shutdown.grace-period

			public @Nullable Duration getIdle() {
				return this.idle;
			}

			public void setIdle(@Nullable Duration idle) {
				this.idle = idle;
			}

			public @Nullable Duration getAge() {
				return this.age;
			}

			public void setAge(@Nullable Duration age) {
				this.age = age;
			}

			public @Nullable Duration getGrace() {
				return this.grace;
			}

			public void setGrace(@Nullable Duration grace) {
				this.grace = grace;
			}

		}

	}

	public static class Ssl {

		/**
		 * Whether to enable SSL support.
		 */
		private @Nullable Boolean enabled;

		/**
		 * Client authentication mode.
		 */
		private ClientAuth clientAuth = ClientAuth.NONE;

		/**
		 * SSL bundle name. Should match a bundle configured in spring.ssl.bundle.
		 */
		private @Nullable String bundle;

		/**
		 * Flag to indicate that client authentication is secure (i.e. certificates are
		 * checked). Do not set this to false in production.
		 */
		private boolean secure = true;

		public @Nullable Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(@Nullable Boolean enabled) {
			this.enabled = enabled;
		}

		/**
		 * Determine whether to enable SSL support. When the {@code enabled} property is
		 * specified it determines enablement. Otherwise, the support is enabled if the
		 * {@code bundle} is provided.
		 * @return whether to enable SSL support
		 */
		public boolean determineEnabled() {
			return (this.enabled != null) ? this.enabled : this.bundle != null;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

		public void setClientAuth(ClientAuth clientAuth) {
			this.clientAuth = clientAuth;
		}

		public ClientAuth getClientAuth() {
			return this.clientAuth;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public boolean isSecure() {
			return this.secure;
		}

	}

}
