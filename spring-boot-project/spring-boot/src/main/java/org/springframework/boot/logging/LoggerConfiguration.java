/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.logging;

import java.util.Objects;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Immutable class that represents the configuration of a {@link LoggingSystem}'s logger.
 *
 * @author Ben Hale
 * @author Phillip Webb
 * @since 1.5.0
 */
public final class LoggerConfiguration {

	private final String name;

	private final LevelConfiguration assignedLevelConfiguration;

	private final LevelConfiguration effectiveLevelConfiguration;

	/**
	 * Create a new {@link LoggerConfiguration instance}.
	 * @param name the name of the logger
	 * @param configuredLevel the configured level of the logger
	 * @param effectiveLevel the effective level of the logger
	 */
	public LoggerConfiguration(String name, LogLevel configuredLevel, LogLevel effectiveLevel) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(effectiveLevel, "EffectiveLevel must not be null");
		this.name = name;
		this.assignedLevelConfiguration = (configuredLevel != null) ? LevelConfiguration.of(configuredLevel) : null;
		this.effectiveLevelConfiguration = LevelConfiguration.of(effectiveLevel);
	}

	/**
	 * Create a new {@link LoggerConfiguration instance}.
	 * @param name the name of the logger
	 * @param assignedLevelConfiguration the assigned level configuration
	 * @param effectiveLevelConfiguration the effective level configuration
	 * @since 2.7.13
	 */
	public LoggerConfiguration(String name, LevelConfiguration assignedLevelConfiguration,
			LevelConfiguration effectiveLevelConfiguration) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(effectiveLevelConfiguration, "EffectiveLevelConfiguration must not be null");
		this.name = name;
		this.assignedLevelConfiguration = assignedLevelConfiguration;
		this.effectiveLevelConfiguration = effectiveLevelConfiguration;
	}

	/**
	 * Returns the name of the logger.
	 * @return the name of the logger
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the configured level of the logger.
	 * @return the configured level of the logger
	 * @see #getAssignedLevelConfiguration()
	 */
	public LogLevel getConfiguredLevel() {
		LevelConfiguration configuration = getAssignedLevelConfiguration();
		return (configuration != null) ? configuration.getLevel() : null;
	}

	/**
	 * Returns the effective level of the logger.
	 * @return the effective level of the logger
	 * @see #getEffectiveLevelConfiguration()
	 */
	public LogLevel getEffectiveLevel() {
		return getEffectiveLevelConfiguration().getLevel();
	}

	/**
	 * Return the level configuration explicitly assigned to the logger or {@code null} if
	 * only the effective level configuration is available.
	 * @return the assigned level configuration or {@code null}
	 * @since 2.7.13
	 */
	public LevelConfiguration getAssignedLevelConfiguration() {
		return this.assignedLevelConfiguration;
	}

	/**
	 * Return the effective level configuration for the logger. This configuration may be
	 * inherited from a parent logger.
	 * @return the effective level configuration
	 * @since 2.7.13
	 */
	public LevelConfiguration getEffectiveLevelConfiguration() {
		return this.effectiveLevelConfiguration;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		if (obj instanceof LoggerConfiguration) {
			LoggerConfiguration other = (LoggerConfiguration) obj;
			return ObjectUtils.nullSafeEquals(this.name, other.name)
					&& ObjectUtils.nullSafeEquals(this.assignedLevelConfiguration, other.assignedLevelConfiguration)
					&& ObjectUtils.nullSafeEquals(this.effectiveLevelConfiguration, other.effectiveLevelConfiguration);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.assignedLevelConfiguration, this.effectiveLevelConfiguration);
	}

	@Override
	public String toString() {
		return "LoggerConfiguration [name=" + this.name + ", configuredLevel=" + this.assignedLevelConfiguration
				+ ", effectiveLevel=" + this.effectiveLevelConfiguration + "]";
	}

	/**
	 * Logger level configuration.
	 *
	 * @since 2.7.13
	 */
	public static class LevelConfiguration {

		private final String name;

		private final LogLevel logLevel;

		private LevelConfiguration(String name, LogLevel logLevel) {
			this.name = name;
			this.logLevel = logLevel;
		}

		/**
		 * Return the name of the level.
		 * @return the level name
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Return the actual level value if possible.
		 * @return the level value
		 * @throws IllegalStateException if this is a {@link #isCustom() custom} level
		 */
		public LogLevel getLevel() {
			Assert.state(this.logLevel != null, "Unable to provide LogLevel for '" + this.name + "'");
			return this.logLevel;
		}

		/**
		 * Return if this is a custom level and cannot be represented by {@link LogLevel}.
		 * @return if this is a custom level
		 */
		public boolean isCustom() {
			return this.logLevel == null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.logLevel, this.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			LevelConfiguration other = (LevelConfiguration) obj;
			return this.logLevel == other.logLevel && ObjectUtils.nullSafeEquals(this.name, other.name);
		}

		@Override
		public String toString() {
			return "LevelConfiguration [name=" + this.name + ", logLevel=" + this.logLevel + "]";
		}

		/**
		 * Create a new {@link LevelConfiguration} instance of the given {@link LogLevel}.
		 * @param logLevel the log level
		 * @return a new {@link LevelConfiguration} instance
		 */
		public static LevelConfiguration of(LogLevel logLevel) {
			Assert.notNull(logLevel, "LogLevel must not be null");
			return new LevelConfiguration(logLevel.name(), logLevel);
		}

		/**
		 * Create a new {@link LevelConfiguration} instance for a custom level name.
		 * @param name the log level name
		 * @return a new {@link LevelConfiguration} instance
		 */
		public static LevelConfiguration ofCustom(String name) {
			Assert.hasText(name, "Name must not be empty");
			return new LevelConfiguration(name, null);
		}

	}

}
