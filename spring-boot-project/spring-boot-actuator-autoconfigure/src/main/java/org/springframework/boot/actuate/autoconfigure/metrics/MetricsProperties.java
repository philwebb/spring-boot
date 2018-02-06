/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link ConfigurationProperties} for configuring Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties("management.metrics")
public class MetricsProperties {

	/**
	 * Whether auto-configured MeterRegistry implementations should be bound to the global
	 * static registry on Metrics. For testing, set this to 'false' to maximize test
	 * independence.
	 */
	private boolean useGlobalRegistry = true;

	/**
	 * Meter IDs to be explicitly enabled or disabled. Any meters with an ID that
	 * starts-with, or equals a key will be configured (the longest match wins).The key
	 * `all` can also be used to configure all meters.
	 */
	private Map<String, Boolean> enable = new LinkedHashMap<>();;

	private final Web web = new Web();

	private final Distribution distribution = new Distribution();

	public boolean isUseGlobalRegistry() {
		return this.useGlobalRegistry;
	}

	public void setUseGlobalRegistry(boolean useGlobalRegistry) {
		this.useGlobalRegistry = useGlobalRegistry;
	}

	public Map<String, Boolean> getEnable() {
		return this.enable;
	}

	public void setEnable(Map<String, Boolean> enable) {
		Assert.notNull(enable, "enable must not be null");
		this.enable = enable;
	}

	public Web getWeb() {
		return this.web;
	}

	public Distribution getDistribution() {
		return this.distribution;
	}

	public static class Web {

		private final Client client = new Client();

		private final Server server = new Server();

		public Client getClient() {
			return this.client;
		}

		public Server getServer() {
			return this.server;
		}

		public static class Client {

			/**
			 * Whether instrumented requests record percentiles histogram buckets by
			 * default.
			 */
			private boolean recordRequestPercentiles;

			/**
			 * Name of the metric for sent requests.
			 */
			private String requestsMetricName = "http.client.requests";

			/**
			 * Maximum number of unique URI tag values allowed. After the max number of
			 * tag values is reached, metrics with additional tag values are denied by
			 * filter.
			 */
			private int maxUriTags = 100;

			public boolean isRecordRequestPercentiles() {
				return this.recordRequestPercentiles;
			}

			public void setRecordRequestPercentiles(boolean recordRequestPercentiles) {
				this.recordRequestPercentiles = recordRequestPercentiles;
			}

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

			public int getMaxUriTags() {
				return this.maxUriTags;
			}

			public void setMaxUriTags(int maxUriTags) {
				this.maxUriTags = maxUriTags;
			}

		}

		public static class Server {

			/**
			 * Whether requests handled by Spring MVC or WebFlux should be automatically
			 * timed. If the number of time series emitted grows too large on account of
			 * request mapping timings, disable this and use 'Timed' on a per request
			 * mapping basis as needed.
			 */
			private boolean autoTimeRequests = true;

			/**
			 * Name of the metric for received requests.
			 */
			private String requestsMetricName = "http.server.requests";

			public boolean isAutoTimeRequests() {
				return this.autoTimeRequests;
			}

			public void setAutoTimeRequests(boolean autoTimeRequests) {
				this.autoTimeRequests = autoTimeRequests;
			}

			public String getRequestsMetricName() {
				return this.requestsMetricName;
			}

			public void setRequestsMetricName(String requestsMetricName) {
				this.requestsMetricName = requestsMetricName;
			}

		}

	}

	public static class Distribution {

		/**
		 * Meter IDs with specific publish histogram configuration. Monitoring systems
		 * that support aggregable percentile calculation based on a histogram be set to
		 * true. For other systems, this has no effect. Any meters with an ID that
		 * starts-with, or equals a key will be configured (the longest match wins).The
		 * key `all` can also be used to configure all meters.
		 */
		private Map<String, Boolean> histogram = new LinkedHashMap<>();

		/**
		 * Meter IDs with a specific set of Micrometer-computed non-aggregable percentiles
		 * to ship to the backend. Any meters with an ID that starts-with, or equals a key
		 * will be configured (the longest match wins).The key `all` can also be used to
		 * configure all meters.
		 */
		private Map<String, double[]> percentiles = new LinkedHashMap<>();;

		/**
		 * Meter IDs that should publish a counter for each SLA boundary specified. Any
		 * meters with an ID that starts-with, or equals a key will be configured (the
		 * longest match wins).The key `all` can also be used to configure all meters.
		 * Values can be specified as a long or as a Duration value (for timer meters,
		 * defaulting to ms if no unit specified).
		 */
		private Map<String, ServiceLevelAgreementBoundary[]> sla = new LinkedHashMap<>();;

		public Map<String, Boolean> getHistogram() {
			return this.histogram;
		}

		public void setHistogram(Map<String, Boolean> histogram) {
			Assert.notNull(histogram, "Histogram must not be null");
			this.histogram = histogram;
		}

		public Map<String, double[]> getPercentiles() {
			return this.percentiles;
		}

		public void setPercentiles(Map<String, double[]> percentiles) {
			Assert.notNull(percentiles, "Percentiles must not be null");
			this.percentiles = percentiles;
		}

		public Map<String, ServiceLevelAgreementBoundary[]> getSla() {
			return this.sla;
		}

		public void setSla(Map<String, ServiceLevelAgreementBoundary[]> sla) {
			Assert.notNull(sla, "SLA must not be null");
			this.sla = sla;
		}

	}

}
