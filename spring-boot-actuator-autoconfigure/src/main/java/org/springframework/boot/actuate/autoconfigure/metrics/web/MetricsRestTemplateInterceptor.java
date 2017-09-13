/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.hist.Histogram;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

/**
 * Intercepts {@link RestTemplate} requests and records metrics about execution time and
 * results.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class MetricsRestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private final MeterRegistry meterRegistry;

	private final RestTemplateTagConfigurer tagProvider;

	private final MetricsProperties properties;

	public MetricsRestTemplateInterceptor(MeterRegistry meterRegistry,
			RestTemplateTagConfigurer tagProvider, MetricsProperties properties) {
		this.tagProvider = tagProvider;
		this.meterRegistry = meterRegistry;
		this.properties = properties;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		long startTime = System.nanoTime();
		ClientHttpResponse response = null;
		try {
			response = execution.execute(request, body);
			return response;
		}
		finally {
			getTimeBuilder(request, response).register(this.meterRegistry)
					.record(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
		}
	}

	private Timer.Builder getTimeBuilder(HttpRequest request,
			ClientHttpResponse response) {
		Timer.Builder builder = Timer
				.builder(this.properties.getWeb().getClientRequestsName())
				.tags(this.tagProvider.clientHttpRequestTags(request, response))
				.description("Timer of RestTemplate operation");
		if (this.properties.getWeb().getClientRequestPercentiles()) {
			builder = builder.histogram(Histogram.percentiles());
		}
		return builder;
	}

}
