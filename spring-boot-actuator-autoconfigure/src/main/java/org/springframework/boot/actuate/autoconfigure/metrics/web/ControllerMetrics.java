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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.hist.Histogram;
import io.micrometer.core.instrument.stats.quantile.WindowSketchQuantiles;
import io.micrometer.core.instrument.util.AnnotationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.method.HandlerMethod;

/**
 * Support class for Controller-related metrics.
 *
 * @author Jon Schneider
 */
class ControllerMetrics {

	private static final String TIMING_REQUEST_ATTRIBUTE = "micrometer.requestStartTime";

	private static final String HANDLER_REQUEST_ATTRIBUTE = "micrometer.requestHandler";

	private static final String EXCEPTION_ATTRIBUTE = "micrometer.requestException";

	private static final Log logger = LogFactory.getLog(ControllerMetrics.class);

	private final MeterRegistry registry;

	private MetricsProperties properties;

	private final WebServletTagConfigurer tagConfigurer;

	private final Map<HttpServletRequest, Long> longTaskTimerIds = Collections
			.synchronizedMap(new IdentityHashMap<>());

	ControllerMetrics(MeterRegistry registry, MetricsProperties properties,
			WebServletTagConfigurer tagConfigurer) {
		this.registry = registry;
		this.properties = properties;
		this.tagConfigurer = tagConfigurer;
	}

	void tagWithException(Throwable t) {
		RequestContextHolder.getRequestAttributes().setAttribute(EXCEPTION_ATTRIBUTE, t,
				RequestAttributes.SCOPE_REQUEST);
	}

	void preHandle(HttpServletRequest request, Object handler) {
		request.setAttribute(TIMING_REQUEST_ATTRIBUTE, System.nanoTime());
		request.setAttribute(HANDLER_REQUEST_ATTRIBUTE, handler);

		longTaskTimed(handler).forEach((t) -> {
			if (t.name == null) {
				if (handler instanceof HandlerMethod) {
					logger.warn("Unable to perform metrics timing on "
							+ ((HandlerMethod) handler).getShortLogMessage()
							+ ": @Timed annotation must have a value used to name the metric");
				}
				else {
					logger.warn("Unable to perform metrics timing for request "
							+ request.getRequestURI()
							+ ": @Timed annotation must have a value used to name the metric");
				}
				return;
			}
			this.longTaskTimerIds.put(request,
					longTaskTimer(t, request, handler).start());
		});
	}

	void record(HttpServletRequest request, HttpServletResponse response, Throwable ex) {
		Long startTime = (Long) request.getAttribute(TIMING_REQUEST_ATTRIBUTE);
		Object handler = request.getAttribute(HANDLER_REQUEST_ATTRIBUTE);

		long endTime = System.nanoTime();
		Throwable thrown = ex != null ? ex
				: (Throwable) request.getAttribute(EXCEPTION_ATTRIBUTE);

		// complete any LongTaskTimer tasks running for this method
		longTaskTimed(handler).forEach((t) -> {
			if (t.name != null) {
				longTaskTimer(t, request, handler)
						.stop(this.longTaskTimerIds.remove(request));
			}
		});

		// record Timer values
		timed(handler).forEach((t) -> {
			Timer.Builder timerBuilder = Timer.builder(t.name)
					.tags(this.tagConfigurer.httpRequestTags(request, response, thrown))
					.tags(t.extraTags).description("Timer of servlet request");

			if (t.quantiles.length > 0) {
				timerBuilder = timerBuilder
						.quantiles(WindowSketchQuantiles.quantiles(t.quantiles).create());
			}

			if (t.percentiles) {
				timerBuilder = timerBuilder.histogram(Histogram.percentilesTime());
			}

			timerBuilder.register(this.registry).record(endTime - startTime,
					TimeUnit.NANOSECONDS);
		});
	}

	private LongTaskTimer longTaskTimer(TimerConfig t, HttpServletRequest request,
			Object handler) {
		Iterable<Tag> tags = Tags.concat(
				this.tagConfigurer.httpLongRequestTags(request, handler), t.extraTags);
		return this.registry.more().longTaskTimer(
				this.registry.createId(t.name, tags, "Timer of long servlet request"));
	}

	private Set<TimerConfig> longTaskTimed(Object m) {
		if (!(m instanceof HandlerMethod)) {
			return Collections.emptySet();
		}

		Set<TimerConfig> timed = AnnotationUtils
				.findTimed(((HandlerMethod) m).getMethod()).filter(Timed::longTask)
				.map(this::fromAnnotation).collect(Collectors.toSet());
		if (timed.isEmpty()) {
			return AnnotationUtils.findTimed(((HandlerMethod) m).getBeanType())
					.filter(Timed::longTask).map(this::fromAnnotation)
					.collect(Collectors.toSet());
		}
		return timed;
	}

	private Set<TimerConfig> timed(Object m) {
		if (!(m instanceof HandlerMethod)) {
			return Collections.emptySet();
		}

		Set<TimerConfig> timed = AnnotationUtils
				.findTimed(((HandlerMethod) m).getMethod()).filter((t) -> !t.longTask())
				.map(this::fromAnnotation).collect(Collectors.toSet());
		if (timed.isEmpty()) {
			timed = AnnotationUtils.findTimed(((HandlerMethod) m).getBeanType())
					.filter((t) -> !t.longTask()).map(this::fromAnnotation)
					.collect(Collectors.toSet());
			if (timed.isEmpty() && this.properties.getWeb().getAutoTimeServerRequests()) {
				return Collections.singleton(new TimerConfig());
			}
		}

		return timed;
	}

	private TimerConfig fromAnnotation(Timed timed) {
		TimerConfig c = new TimerConfig();
		c.name = timed.value().isEmpty()
				? this.properties.getWeb().getServerRequestsName() : timed.value();
		if (c.longTask && timed.value().isEmpty()) {
			c.name = null; // the user MUST name long task timers, we don't lump them in
							// with regular timers with the same name
		}

		c.extraTags = Tags.zip(timed.extraTags());
		c.longTask = timed.longTask();
		c.quantiles = timed.quantiles();
		c.percentiles = timed.percentiles();
		return c;
	}

	private class TimerConfig {

		String name = ControllerMetrics.this.properties.getWeb().getServerRequestsName();

		Iterable<Tag> extraTags = Collections.emptyList();

		boolean longTask = false;

		double[] quantiles = new double[0];

		boolean percentiles = ControllerMetrics.this.properties.getWeb()
				.getServerRequestPercentiles();

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TimerConfig that = (TimerConfig) o;

			return this.name != null ? this.name.equals(that.name) : that.name == null;
		}

		@Override
		public int hashCode() {
			return this.name != null ? this.name.hashCode() : 0;
		}
	}

}
