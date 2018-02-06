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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServiceLevelAgreementBoundary}.
 *
 * @author Phillip Webb
 */
public class ServiceLevelAgreementBoundaryTests {

	@Test
	public void getValueWhenFromLongShouldReturnValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf(123L);
		assertThat(sla.getValue()).isEqualTo(123);
	}

	@Test
	public void getValueWhenFromNumberStringShouldReturnValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123");
		assertThat(sla.getValue()).isEqualTo(123);
	}

	@Test
	public void getValueWhenFromDurationStringShouldReturnNanosValue() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("10ms");
		assertThat(sla.getValue()).isEqualTo(10000000);
	}

	@Test
	public void isApplicableWhenFromNumberShouldBeApplicableOnlyToTimerAndDistributation() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("123");
		for (Meter.Type type : Meter.Type.values()) {
			assertThat(sla.isApplicable(type))
					.isEqualTo(type == Type.DISTRIBUTION_SUMMARY || type == Type.TIMER);
		}
	}

	@Test
	public void isApplicableWhenFromDurationShouldBeApplicableOnlyToTimer() {
		ServiceLevelAgreementBoundary sla = ServiceLevelAgreementBoundary.valueOf("1s");
		for (Meter.Type type : Meter.Type.values()) {
			assertThat(sla.isApplicable(type)).isEqualTo(type == Type.TIMER);
		}
	}

	@Test
	public void valueOfShouldWorkInBinder() {
		MockEnvironment environment = new MockEnvironment();
		TestPropertyValues.of("duration=10ms", "long=10").applyTo(environment);
		assertThat(Binder.get(environment)
				.bind("duration", Bindable.of(ServiceLevelAgreementBoundary.class)).get()
				.getValue()).isEqualTo(10000000);
		assertThat(Binder.get(environment)
				.bind("long", Bindable.of(ServiceLevelAgreementBoundary.class)).get()
				.getValue()).isEqualTo(10);
	}

}
