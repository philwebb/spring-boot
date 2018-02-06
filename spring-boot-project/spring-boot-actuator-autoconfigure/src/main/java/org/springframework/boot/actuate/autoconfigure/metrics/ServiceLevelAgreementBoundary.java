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

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

import io.micrometer.core.instrument.Meter;

import org.springframework.boot.context.properties.bind.convert.DurationConverter;

/**
 * A service level agreement boundary for use when configuring micrometer. Can be
 * specified as either a {@link Long} (applicable to timers and distribution summaries) or
 * a {@link Long} (applicable to only timers).
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class ServiceLevelAgreementBoundary {

	private final long value;

	private final Set<Meter.Type> types;

	ServiceLevelAgreementBoundary(long value) {
		this.value = value;
		this.types = EnumSet.of(Meter.Type.DISTRIBUTION_SUMMARY, Meter.Type.TIMER);
	}

	ServiceLevelAgreementBoundary(Duration value) {
		this.value = value.toNanos();
		this.types = EnumSet.of(Meter.Type.TIMER);
	}

	/**
	 * Return the underlying value of the SLA. If sourced from a {@link Duration} this
	 * value will be in nanoseconds.
	 * @return the value
	 */
	public long getValue() {
		return this.value;
	}

	/**
	 * Determine if the {@link ServiceLevelAgreementBoundary} is applicable for the given
	 * meter type.
	 * @param meterType the meter type
	 * @return if the SLA is applicable
	 */
	public boolean isApplicable(Meter.Type meterType) {
		return this.types.contains(meterType);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value == ((ServiceLevelAgreementBoundary) obj).value;
	}

	public static ServiceLevelAgreementBoundary valueOf(String value) {
		if (isNumber(value)) {
			return new ServiceLevelAgreementBoundary(Long.parseLong(value));
		}
		return new ServiceLevelAgreementBoundary(
				DurationConverter.toDuration(value, null));
	}

	/**
	 * Return a new {@link ServiceLevelAgreementBoundary} instance for the given long
	 * value.
	 * @param value the source value
	 * @return a {@link ServiceLevelAgreementBoundary} instance
	 */
	public static ServiceLevelAgreementBoundary valueOf(long value) {
		return new ServiceLevelAgreementBoundary(value);
	}

	/**
	 * Return a new {@link ServiceLevelAgreementBoundary} instance for the given String
	 * value. The value may contain a simple number, or a {@link DurationConverter
	 * duration formatted value}
	 * @param value the source value
	 * @return a {@link ServiceLevelAgreementBoundary} instance
	 */
	private static boolean isNumber(String value) {
		return value.chars().allMatch(Character::isDigit);
	}

}
