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

import io.grpc.Status;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataUnit;

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
 * @since 4.1.0
 * @see GrpcClientDefaultServiceConfigCustomizer
 * @see io.grpc.internal.ServiceConfigUtil
 */
public record ServiceConfig(@Nullable List<LoadBalancingConfig> loadbalancing, List<MethodConfig> method,
		RetryThrottlingPolicy retrythrottling, HealthcheckConfig healthcheck) {

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
			.as(LoadBalancingConfig::grpcJavaConfigs)
			.to(grpcJavaConfig.in("loadBalancingConfig"));
	}

	// ServiceConfigUtil

	public record LoadBalancingConfig(PickFirstLoadBalancingConfig pickfirst, RoundRobinLoadBalancingConfig roundrobin,
			WeightedRoundRobinLoadBalancingConfig weightedroundrobin) {

		Map<String, Object> grpcJavaConfig() {
			LinkedHashMap<String, Object> grpcJavaConfig = new LinkedHashMap<>();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::pickfirst)
				.as(PickFirstLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("pick_first", loadBalancingConfig));
			map.from(this::roundrobin)
				.as(RoundRobinLoadBalancingConfig::grpcJavaConfig)
				.to((loadBalancingConfig) -> grpcJavaConfig.put("round_robin", loadBalancingConfig));
			return grpcJavaConfig;
		}

		static @Nullable List<Map<String, Object>> grpcJavaConfigs(List<LoadBalancingConfig> loadBalancingConfigs) {
			return (!CollectionUtils.isEmpty(loadBalancingConfigs))
					? loadBalancingConfigs.stream().map(LoadBalancingConfig::grpcJavaConfig).toList() : null;
		}

	}

	// PickFirstLoadBalancerProvider
	// PickFirstLoadBalancerConfig + shuffleAddressList
	public record PickFirstLoadBalancingConfig(Boolean shuffleAddressList) {

		Map<String, Object> grpcJavaConfig() {
			GrpcJavaConfig grpcJavaConfig = new GrpcJavaConfig();
			PropertyMapper map = PropertyMapper.get();
			map.from(this::shuffleAddressList).to(grpcJavaConfig.in("shuffleAddressList"));
			return grpcJavaConfig.getMap();
		}

	}

	// SecretRoundRobinLoadBalancerProvider
	public record RoundRobinLoadBalancingConfig() {

		Map<String, Object> grpcJavaConfig() {
			return Collections.emptyMap();
		}
	}

	// WeightedRoundRobinLoadBalancerProvider
	public record WeightedRoundRobinLoadBalancingConfig(Duration blackoutPeriod, Duration weightExpirationPeriod,
			Duration outOfBandReportingPeriod, Boolean enableOutOfBandLoadReport, Duration weightUpdatePeriod,
			Float errorUtilizationPenalty) {

	}

	// GrpclbLoadBalancerProvider
	public record GrpcLoadBalancingConfig(LoadBalancingConfig childPolicy, String serviceName,
			Duration initialFallbackTimeout) {

	}

	public record MethodConfig(List<Name> name, Boolean waitForReady, DataUnit maxRequestMessage,
			DataUnit maxResponseMessage, Duration timeout, RetryPolicy retryPolicy, HedgingPolicy hedgingPolicy) {

		public record Name(String service, String method) {

		}

		public record RetryPolicy(Integer maxAttempts, Duration initialBackoff, Duration maxBackoff,
				Double backoffMultiplier, Duration perAttemptRecvTimeout, Set<Status.Code> retryableStatusCodes) {

		}

		public record HedgingPolicy(Integer maxAttempts, Duration hedgingDelay, Set<Status.Code> nonFatalStatusCodes) {

		}
	}

	public record RetryThrottlingPolicy(Float maxTokens, Float tokenRation) {

	}

	public record HealthcheckConfig(String serviceName) {

	}

	static class GrpcJavaConfig {

		private final Map<String, Object> map;

		GrpcJavaConfig() {
			this(new LinkedHashMap<>());
		}

		GrpcJavaConfig(Map<String, Object> map) {
			this.map = map;
		}

		<T> Consumer<T> in(String key) {
			return (value) -> this.map.put(key, value);
		}

		Map<String, Object> getMap() {
			return this.map;
		}

	}

}
