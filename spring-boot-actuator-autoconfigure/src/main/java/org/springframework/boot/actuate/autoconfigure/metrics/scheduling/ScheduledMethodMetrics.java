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

package org.springframework.boot.actuate.autoconfigure.metrics.scheduling;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.stats.quantile.WindowSketchQuantiles;
import io.micrometer.core.instrument.util.AnnotationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * {@link Around} advice for recording metrics for {@link Scheduled} methods.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Aspect
public class ScheduledMethodMetrics {

	private static final Log logger = LogFactory.getLog(ScheduledMethodMetrics.class);

	private final MeterRegistry registry;

	public ScheduledMethodMetrics(MeterRegistry registry) {
		this.registry = registry;
	}

	@Around("execution (@org.springframework.scheduling.annotation.Scheduled  * *.*(..))")
	public Object timeScheduledOperation(ProceedingJoinPoint joinPoint) throws Throwable {
		Signature signature = joinPoint.getSignature();
		Method method = ((MethodSignature) signature).getMethod();
		if (method.getDeclaringClass().isInterface()) {
			try {
				Class<?> targetClass = joinPoint.getTarget().getClass();
				method = targetClass.getDeclaredMethod(signature.getName(),
						method.getParameterTypes());
			}
			catch (SecurityException | NoSuchMethodException ex) {
				logger.warn("Unable to perform metrics timing on "
						+ signature.toShortString(), ex);
				return joinPoint.proceed();
			}
		}
		Timers timers = new Timers(this.registry, method);
		if (timers.hasShort() && timers.hasLong()) {
			return record(timers.getLong(),
					() -> record(timers.getShort(), joinPoint::proceed));
		}
		if (timers.hasShort()) {
			return record(timers.getShort(), joinPoint::proceed);
		}
		if (timers.hasLong()) {
			return record(timers.getLong(), joinPoint::proceed);
		}
		return joinPoint.proceed();
	}

	private Object record(LongTaskTimer timer, ThrowableCallable callable)
			throws Throwable {
		long id = timer.start();
		try {
			return callable.call();
		}
		finally {
			timer.stop(id);
		}
	}

	private Object record(Timer timer, ThrowableCallable callable) throws Throwable {
		Clock clock = this.registry.config().clock();
		long start = clock.monotonicTime();
		try {
			return callable.call();
		}
		finally {
			timer.record(clock.monotonicTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	private static class Timers {

		private Timer shortTaskTimer;

		private LongTaskTimer longTaskTimer;

		public Timers(MeterRegistry registry, Method method) {
			for (Timed timed : AnnotationUtils.findTimed(method).toArray(Timed[]::new)) {
				process(registry, timed);
			}
		}

		private void process(MeterRegistry registry, Timed timed) {
			if (timed.longTask()) {
				this.longTaskTimer = createLongTaskTimer(registry, timed);
			}
			else {
				this.shortTaskTimer = createShortTaskTimer(registry, timed);
			}
		}

		private LongTaskTimer createLongTaskTimer(MeterRegistry registry, Timed timed) {
			List<Tag> tags = Tags.zip(timed.extraTags());
			Id id = registry.createId(timed.value(), tags,
					"Timer of @Scheduled long task");
			return registry.more().longTaskTimer(id);
		}

		private Timer createShortTaskTimer(MeterRegistry registry, Timed timed) {
			String[] tags = timed.extraTags();
			String description = "Timer of @Scheduled task";
			Timer.Builder builder = Timer.builder(timed.value()).tags(tags)
					.description(description);
			if (timed.quantiles().length > 0) {
				WindowSketchQuantiles quantiles = WindowSketchQuantiles
						.quantiles(timed.quantiles()).create();
				builder = builder.quantiles(quantiles);
			}
			return builder.register(registry);
		}

		public boolean hasShort() {
			return this.shortTaskTimer != null;
		}

		public Timer getShort() {
			return this.shortTaskTimer;
		}

		public boolean hasLong() {
			return this.longTaskTimer != null;
		}

		public LongTaskTimer getLong() {
			return this.longTaskTimer;
		}

	}

	private interface ThrowableCallable {

		Object call() throws Throwable;

	}

}
