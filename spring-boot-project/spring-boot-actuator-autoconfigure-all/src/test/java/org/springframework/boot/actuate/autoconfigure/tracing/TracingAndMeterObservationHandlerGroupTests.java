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
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.observation.autoconfigure.ObservationHandlerGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TracingAndMeterObservationHandlerGroup}.
 *
 * @author Phillip Webb
 */
class TracingAndMeterObservationHandlerGroupTests {

	@Test
	void compareToSortsBeforeMeterObservationHandlerGroup() {
		ObservationHandlerGroup meterGroup = ObservationHandlerGroup.of(MeterObservationHandler.class);
		TracingAndMeterObservationHandlerGroup tracingAndMeterGroup = new TracingAndMeterObservationHandlerGroup(
				mock(Tracer.class));
		assertThat(sort(meterGroup, tracingAndMeterGroup)).containsExactly(tracingAndMeterGroup, meterGroup);
		assertThat(sort(tracingAndMeterGroup, meterGroup)).containsExactly(tracingAndMeterGroup, meterGroup);
	}

	@Test
	void isMemberAcceptsMeterObservationHandlerOrTracingObservationHandler() {
		TracingAndMeterObservationHandlerGroup group = new TracingAndMeterObservationHandlerGroup(mock(Tracer.class));
		assertThat(group.isMember(mock(ObservationHandler.class))).isFalse();
		assertThat(group.isMember(mock(MeterObservationHandler.class))).isTrue();
		assertThat(group.isMember(mock(TracingObservationHandler.class))).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerMembersWrapsMeterObservationHandlers() {
		Tracer tracer = mock(Tracer.class);
		TracingAndMeterObservationHandlerGroup group = new TracingAndMeterObservationHandlerGroup(tracer);
		TracingObservationHandler<?> tracingHandler = mock(TracingObservationHandler.class);
		MeterObservationHandler<?> meterHandler = mock(MeterObservationHandler.class);
		ObservationConfig config = mock(ObservationConfig.class);
		List<ObservationHandler<?>> members = List.of(tracingHandler, meterHandler);
		group.registerMembers(config, members);
		ArgumentCaptor<ObservationHandler<?>> handlerCaptor = ArgumentCaptor.captor();
		then(config).should().observationHandler(handlerCaptor.capture());
		List<ObservationHandler<?>> actualComposite = handlerCaptor.getAllValues();
		assertThat(actualComposite).hasSize(1);
		assertThat(actualComposite.get(0)).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		List<ObservationHandler<?>> handlers = (List<ObservationHandler<?>>) Extractors.byName("handlers")
			.apply(handlerCaptor.getValue());
		assertThat(handlers).hasSize(2);
		assertThat(handlers.get(0)).isSameAs(tracingHandler);
		assertThat(handlers.get(1)).isInstanceOf(TracingAwareMeterObservationHandler.class);
		assertThat(handlers.get(1)).extracting("delegate").isSameAs(meterHandler);
		assertThat(handlers.get(1)).extracting("tracer").isSameAs(tracer);
	}

	private List<ObservationHandlerGroup> sort(ObservationHandlerGroup... groups) {
		List<ObservationHandlerGroup> list = new ArrayList<>(List.of(groups));
		Collections.sort(list);
		return list;
	}

}
