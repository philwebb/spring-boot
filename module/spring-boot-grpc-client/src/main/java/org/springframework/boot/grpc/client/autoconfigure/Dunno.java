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

/**
 * @author pwebb
 * @see https://github.com/grpc/grpc-proto/blob/master/grpc/service_config/service_config.proto
 */
public class Dunno {

	public static class MethodConfig {

		private List<Name> names;

		private Boolean waitForReady;

		private Duration timeout;

		private Integer maxRequestMessageBytes;

		private Integer maxResponseMessageBytes;

		public static class Name {

			private String service;

			private String method;

		}

		public static class RetryPolicy {

			Integer maxAttempts;

			Duration maxBackoff;

			Float backoffMultiplier;

			Object retryable_status_codes;

		}

		public static class HedgingPolicy {

			uint32 max_attempts = 1;

			google.protobuf.Duration hedging_delay = 2;

			repeated google.
			rpc.Code non_fatal_status_codes = 3;

		}

	}

}
