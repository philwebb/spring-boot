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
import java.util.List;
import java.util.Set;

import io.grpc.Status;
import io.grpc.internal.ServiceConfigUtil;

import org.springframework.util.unit.DataUnit;

/**
 * @author pwebb
 * @see ServiceConfigUtil
 * @see https://github.com/grpc/grpc-proto/blob/master/grpc/service_config/service_config.proto
 */
public record ServiceConfig(List<LoadBalancingConfig> loadBalancing, List<MethodConfig> method,
		RetryThrottlingPolicy retryThrottling, HealthCheckConfig healthCheck) {

	public record LoadBalancingConfig(Xds xds, RoundRobin roundRobin) {
		// FIXME Too complex

		/*
		 * In Java there's a Map of "name" -> "config"
		 * io.grpc.LoadBalancerRegistry.getProvider(String) gets the policy
		 *
		 *
		 *
		 */

		public record Xds() {

		}

		public record RoundRobin() {

		}

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

	public record HealthCheckConfig(String serviceName) {

	}

}
