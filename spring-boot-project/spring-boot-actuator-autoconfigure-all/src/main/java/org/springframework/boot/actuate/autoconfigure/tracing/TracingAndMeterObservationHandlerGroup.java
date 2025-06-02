/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;

import org.springframework.boot.observation.autoconfigure.ObservationHandlerGroup;

/**
 * {@link ObservationHandlerGroup} that considers both {@link TracingObservationHandler}
 * and {@link MeterObservationHandler} types as members. This group takes precedence over
 * any regular {@link MeterObservationHandler} group in order to use ensure
 * {@link TracingAwareMeterObservationHandler} wrapping is applied during registration.
 *
 * @author Phillip Webb
 */
class TracingAndMeterObservationHandlerGroup implements ObservationHandlerGroup {

	private final Tracer tracer;

	TracingAndMeterObservationHandlerGroup(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public boolean isMember(ObservationHandler<?> handler) {
		return MeterObservationHandler.class.isInstance(handler) || TracingObservationHandler.class.isInstance(handler);
	}

	@Override
	public int compareTo(ObservationHandlerGroup other) {
		if (other instanceof TracingAndMeterObservationHandlerGroup) {
			return 0;
		}
		return MeterObservationHandler.class.isAssignableFrom(other.handlerType()) ? -1 : 1;
	}

	@Override
	public void registerMembers(ObservationConfig config, List<ObservationHandler<?>> members) {
		List<ObservationHandler<?>> tracingHandlers = new ArrayList<>(members.size());
		for (ObservationHandler<?> handler : members) {
			tracingHandlers.add(asTracingHandler(handler));
		}
		config.observationHandler(new FirstMatchingCompositeObservationHandler(tracingHandlers));
	}

	private ObservationHandler<?> asTracingHandler(ObservationHandler<?> handler) {
		if (handler instanceof MeterObservationHandler<?> delegate
				&& !(handler instanceof TracingAwareMeterObservationHandler<?>)) {
			return new TracingAwareMeterObservationHandler<>(delegate, this.tracer);
		}
		return handler;
	}

	@Override
	public Class<?> handlerType() {
		return TracingObservationHandler.class;
	}

}
