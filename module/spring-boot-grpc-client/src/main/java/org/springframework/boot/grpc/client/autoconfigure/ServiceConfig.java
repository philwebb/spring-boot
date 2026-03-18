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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import io.grpc.Status;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.xds.WeightedRoundRobinLoadBalancerProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.PropertyMapper.Source.Adapter;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Bindable service configuration for gRPC channel. Allows type safe binding of common
 * service configuration options which can ultimately be applied to the {@link Map}
 * provided by a {@link GrpcClientDefaultServiceConfigCustomizer}.
 * <p>
 * The configuration provided here is a subset of the canonical <a href=
 * "https://github.com/grpc/grpc-proto/blob/master/grpc/service_config/service_config.proto">service_config.proto</a>
 * protocol definition. For advanced or experimental service configurations, use the
 * {@link GrpcClientDefaultServiceConfigCustomizer} to directly add any entries supported
 * by {@code grpc-java}.
 *
 * @author Phillip Webb
 * @param loadbalancing load balancing configurations in the order that they should be
 * applied
 * @param method method configuration
 * @param retrythrottling retry throttling policy
 * @param healthcheck health check configuration
 * @since 4.1.0
 * @see GrpcClientDefaultServiceConfigCustomizer
 * @see io.grpc.internal.ServiceConfigUtil
 */
public record ServiceConfig(@Nullable List<LoadBalancingConfig> loadbalancing, @Nullable List<MethodConfig> method,
		@Nullable RetryThrottlingPolicy retrythrottling, @Nullable HealthcheckConfig healthcheck) {

	/**
	 * Apply this service config to the given gRPC Java config Map.
	 * @param grpcJavaConfig the gRPC Java config map
	 */
	public void applyTo(Map<String, Object> grpcJavaConfig) {
		applyTo(new GrpcJavaConfig(grpcJavaConfig));
	}

	private void applyTo(GrpcJavaConfig grpcJavaConfig) {
		PropertyMapper map = PropertyMapper.get();
		map.from(this::loadbalancing)
			.as(list(LoadBalancingConfig::grpcJavaConfig))
			.to(grpcJavaConfig.in("loadBalancingConfig"));
		map.from(this::method).as(list(MethodConfig::grpcJavaConfig)).to(grpcJavaConfig.in("methodConfig"));
	}

	static <T> Adapter<List<T>, @Nullable List<Map<String, Object>>> list(Function<T, Map<String, Object>> adapter) {
		return (list) -> (!CollectionUtils.isEmpty(list)) ? list.stream().map(adapter).toList() : null;
	}

	static String durationString(Duration duration) {
		return duration.getSeconds() + "." + duration.getNano() + "s";
	}

	static String bytesString(DataSize dataSize) {
		return Long.toString(dataSize.toBytes());
	}

	/**
	 * Load balancing config.
	 *
	 * @param pickfirst 'pick first' load balancing
	 * @param roundrobin 'round robin' load balancing
	 * @param weightedroundrobin 'weighted round robin' load balancing
	 * @param grpc 'grpc' load balancing
	 */
	public record LoadBalancingConfig(@Nullable PickFirstLoadBalancingConfig pickfirst,
			@Nullable RoundRobinLoadBalancingConfig roundrobin,
			@Nullable WeightedRoundRobinLoadBalancingConfig weightedroundrobin,
			@Nullable GrpcLoadBalancingConfig grpc) {

		public LoadBalancingConfig

		{
			if (pickfirst == null && roundrobin == null && weightedroundrobin == null && grpc == null) {
				throw new InvalidConfigurationPropertyValueException("loadbalancing", null,
						"Missing load balancing strategy");
			}
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("loadbalancing.pickfirst", pickfirst);
				entries.put("loadbalancing.roundrobin", roundrobin);
				entries.put("loadbalancing.weightedroundrobin", weightedroundrobin);
				entries.put("loadbalancing.grpc", grpc);
			});
		}

		/**
		 * Return the gRPC java config.
		 * @return the config
		 */
		Map<String, Object> grpcJavaConfig() {
			LinkedHashMap<String, Object> grpcJavaConfig = new LinkedHashMap<>();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::pickfirst)
				.as(PickFirstLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("pick_first", loadBalancingConfig));
			map.from(this::roundrobin)
				.as(RoundRobinLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("round_robin", loadBalancingConfig));
			map.from(this::weightedroundrobin)
				.as(WeightedRoundRobinLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("weighted_round_robin", loadBalancingConfig));
			map.from(this::grpc)
				.as(GrpcLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("grpclb", loadBalancingConfig));
			return grpcJavaConfig;
		}

		/**
		 * 'pick first' load balancing.
		 *
		 * @param shuffleAddressList randomly shuffle the list of addresses received from
		 * the name resolver before attempting to connect to them.
		 */
		public record PickFirstLoadBalancingConfig(Boolean shuffleAddressList) {

			/**
			 * Return the gRPC java config as supported by the
			 * {@link PickFirstLoadBalancerProvider}.
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::shuffleAddressList).to(grpcJavaConfig.in("shuffleAddressList"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * 'round robin' load balancing.
		 */
		public record RoundRobinLoadBalancingConfig() {

			/**
			 * Return the gRPC java config as supported by the
			 * {@code SecretRoundRobinLoadBalancerProvider}.
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				return Collections.emptyMap();
			}

		}

		/**
		 * 'weighted round robin' load balancing.
		 *
		 * @param blackoutPeriod must report load metrics continuously for at least this
		 * long before the endpoint weight will be used
		 * @param weightExpirationPeriod if has not reported load metrics in this long,
		 * then we stop using the reported weight
		 * @param outOfBandReportingPeriod load reporting interval to request from the
		 * server
		 * @param enableOutOfBandLoadReport whether to enable out-of-band utilization
		 * reporting collection from the endpoints
		 * @param weightUpdatePeriod how often endpoint weights are recalculated
		 * @param errorUtilizationPenalty multiplier used to adjust endpoint weights with
		 * the error rate calculated as eps/qps
		 */
		public record WeightedRoundRobinLoadBalancingConfig(Duration blackoutPeriod, Duration weightExpirationPeriod,
				Duration outOfBandReportingPeriod, Boolean enableOutOfBandLoadReport, Duration weightUpdatePeriod,
				Float errorUtilizationPenalty) {

			/**
			 * Return the gRPC java config as supported by the
			 * {@link WeightedRoundRobinLoadBalancerProvider}.
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::blackoutPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("blackoutPeriod"));
				map.from(this::weightExpirationPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("weightExpirationPeriod"));
				map.from(this::outOfBandReportingPeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("oobReportingPeriod"));
				map.from(this::enableOutOfBandLoadReport).to(grpcJavaConfig.in("enableOobLoadReport"));
				map.from(this::weightUpdatePeriod)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("weightUpdatePeriod"));
				map.from(this::errorUtilizationPenalty).to(grpcJavaConfig.in("errorUtilizationPenalty"));
				return grpcJavaConfig.asMap();
			}

		}

		/**
		 * 'grpc' load balancing.
		 *
		 * @param child what load balancer policies to use for routing between the backend
		 * addresses
		 * @param serviceName override of the service name to be sent to the balancer
		 * @param initialFallbackTimeout timeout in seconds for receiving the server list
		 */
		public record GrpcLoadBalancingConfig(List<LoadBalancingConfig> child, String serviceName,
				Duration initialFallbackTimeout) {

			public GrpcLoadBalancingConfig

			{
				child.forEach(this::assertChild);
			}

			private void assertChild(LoadBalancingConfig child) {
				if (child.pickfirst() == null && child.roundrobin() == null) {
					throw new InvalidConfigurationPropertyValueException("loadbalancing.grpc.child", null,
							"Only 'pickfirst' or 'roundrobin' child load balancer strategies can be used");
				}
			}

			/**
			 * Return the gRPC java config as supported by the
			 * {@code GrpclbLoadBalancerProvider}.
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				map.from(this::child)
					.as(list(LoadBalancingConfig::grpcJavaConfig))
					.to(grpcJavaConfig.in("childPolicy"));
				map.from(this::serviceName).to(grpcJavaConfig.in("serviceName"));
				map.from(this::initialFallbackTimeout)
					.as(ServiceConfig::durationString)
					.to(grpcJavaConfig.in("initialFallbackTimeout"));
				return grpcJavaConfig.asMap();
			}

		}

	}

	/**
	 * Method configuration.
	 */
	public record MethodConfig(List<Name> name, Boolean waitForReady, DataSize maxRequestMessage,
			DataSize maxResponseMessage, Duration timeout, RetryPolicy retry, HedgingPolicy hedging) {

		static @Nullable List<Map<String, Object>> grpcJavaConfigs(List<MethodConfig> methodConfigs) {
			return (!CollectionUtils.isEmpty(methodConfigs))
					? methodConfigs.stream().map(MethodConfig::grpcJavaConfig).toList() : null;
		}

		/**
		 * Return the gRPC java config
		 * @return the config
		 */
		Map<String, Object> grpcJavaConfig() {
			GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::name).to(grpcJavaConfig.in("name"));
			map.from(this::waitForReady).to(grpcJavaConfig.in("waitForReady"));
			map.from(this::maxRequestMessage)
				.as(ServiceConfig::bytesString)
				.to(grpcJavaConfig.in("maxRequestMessageBytes"));
			map.from(this::maxResponseMessage)
				.as(ServiceConfig::bytesString)
				.to(grpcJavaConfig.in("maxResponseMessageBytes"));
			map.from(this::timeout).as(ServiceConfig::durationString).to(grpcJavaConfig.in("timeout"));
			map.from(this::retry).as(RetryPolicy::grpcJavaConfig).to(grpcJavaConfig.in("retryPolicy"));
			map.from(this::hedging).as(HedgingPolicy::grpcJavaConfig).to(grpcJavaConfig.in("hedgingPolicy"));
			return grpcJavaConfig.asMap();
		}

		public record Name(String service, String method) {
		}

		public record RetryPolicy(Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
				Double backoffMultiplier, Duration perAttemptReceiveTimeout, Set<Status.Code> retryableStatusCodes) {
			/**
			 * Return the gRPC java config
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				return grpcJavaConfig.asMap();
			}

		}

		public record HedgingPolicy(Integer maxAttempts, Duration delay, Set<Status.Code> nonFatalStatusCodes) {
			/**
			 * Return the gRPC java config
			 * @return the config
			 */
			Map<String, Object> grpcJavaConfig() {
				GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
				PropertyMapper map = PropertyMapper.get();
				return grpcJavaConfig.asMap();
			}

		}
	}

	public record RetryThrottlingPolicy(Float maxTokens, Float tokenRation) {

	}

	public record HealthcheckConfig(String serviceName) {

	}

	/**
	 * Internal helper to collection gRPC java config.
	 */
	static record GrpcJavaConfig(Map<String, Object> asMap) {

		GrpcJavaConfig() {
			this(new LinkedHashMap<>());
		}

		<T> Consumer<T> in(String key) {
			return (value) -> this.asMap.put(key, value);
		}

	}

}
