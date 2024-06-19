/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.structured.CommonStructuredLoggingFormats;
import org.springframework.boot.logging.structured.StructuredLoggingFormat;
import org.springframework.boot.logging.structured.StructuredLoggingWriter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link Layout} which writes log events in structured format.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 * @see StructuredLoggingFormat
 */
@Plugin(name = "StructuredLoggingLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class StructuredLoggingLayout extends AbstractStringLayout {

	private final StructuredLoggingFormat format;

	private final Long pid;

	private final String serviceName;

	private final String serviceVersion;

	private final String serviceNodeName;

	private final String serviceEnvironment;

	private final boolean logMdc;

	public StructuredLoggingLayout(StructuredLoggingFormat format, Long pid, String serviceName, String serviceVersion,
			String serviceNodeName, String serviceEnvironment, boolean logMdc) {
		super(StandardCharsets.UTF_8);
		Assert.notNull(format, "Format must not be null");
		this.format = format;
		this.pid = pid;
		this.serviceName = serviceName;
		this.serviceVersion = serviceVersion;
		this.serviceNodeName = serviceNodeName;
		this.serviceEnvironment = serviceEnvironment;
		this.logMdc = logMdc;
	}

	@Override
	public String toSerializable(LogEvent event) {
		StructuredLoggingWriter builder = this.format.createWriter(getStringBuilder());
		this.format.write(new Log4jLogEventAdapter(event), builder);
		builder.newLine();
		return builder.finish();
	}

	@PluginBuilderFactory
	static StructuredLoggingLayout.Builder newBuilder() {
		return new StructuredLoggingLayout.Builder();
	}

	private final class Log4jLogEventAdapter implements org.springframework.boot.logging.structured.LogEvent {

		private final LogEvent event;

		Log4jLogEventAdapter(LogEvent event) {
			this.event = event;
		}

		@Override
		public Instant getTimestamp() {
			org.apache.logging.log4j.core.time.Instant instant = this.event.getInstant();
			return Instant.ofEpochMilli(instant.getEpochMillisecond()).plusNanos(instant.getNanoOfMillisecond());
		}

		@Override
		public String getLevel() {
			return this.event.getLevel().name();
		}

		@Override
		public int getLevelValue() {
			return this.event.getLevel().intLevel();
		}

		@Override
		public String getThreadName() {
			return this.event.getThreadName();
		}

		@Override
		public String getLoggerName() {
			return this.event.getLoggerName();
		}

		@Override
		public String getFormattedMessage() {
			return this.event.getMessage().getFormattedMessage();
		}

		@Override
		public boolean hasThrowable() {
			return this.event.getThrown() != null;
		}

		@Override
		public String getThrowableClassName() {
			return this.event.getThrown().getClass().getName();
		}

		@Override
		public String getThrowableMessage() {
			return this.event.getThrown().getMessage();
		}

		@Override
		public String getThrowableStackTraceAsString() {
			return this.event.getThrownProxy().getExtendedStackTraceAsString();
		}

		@Override
		public Map<String, Object> getKeyValuePairs() {
			return Collections.emptyMap();
		}

		@Override
		public Map<String, String> getMdc() {
			if (!StructuredLoggingLayout.this.logMdc) {
				return Collections.emptyMap();
			}
			ReadOnlyStringMap mdc = this.event.getContextData();
			if (mdc == null || mdc.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<String, String> map = mdc.toMap();
			if (CollectionUtils.isEmpty(map)) {
				return Collections.emptyMap();
			}
			return map;
		}

		@Override
		public Set<String> getMarkers() {
			if (this.event.getMarker() == null) {
				return Collections.emptySet();
			}
			Set<String> result = new HashSet<>();
			addMarker(result, this.event.getMarker());
			return result;
		}

		private void addMarker(Set<String> result, Marker marker) {
			if (marker == null) {
				return;
			}
			result.add(marker.getName());
			if (marker.hasParents()) {
				for (Marker parent : marker.getParents()) {
					addMarker(result, parent);
				}
			}
		}

		@Override
		public Long getPid() {
			return StructuredLoggingLayout.this.pid;
		}

		@Override
		public String getServiceName() {
			return StructuredLoggingLayout.this.serviceName;
		}

		@Override
		public String getServiceVersion() {
			return StructuredLoggingLayout.this.serviceVersion;
		}

		@Override
		public String getServiceEnvironment() {
			return StructuredLoggingLayout.this.serviceEnvironment;
		}

		@Override
		public String getServiceNodeName() {
			return StructuredLoggingLayout.this.serviceNodeName;
		}

	}

	static final class Builder implements org.apache.logging.log4j.core.util.Builder<StructuredLoggingLayout> {

		@PluginBuilderAttribute
		private String format;

		@PluginBuilderAttribute
		private Long pid;

		@PluginBuilderAttribute
		private String serviceName;

		@PluginBuilderAttribute
		private String serviceVersion;

		@PluginBuilderAttribute
		private String serviceNodeName;

		@PluginBuilderAttribute
		private String serviceEnvironment;

		@PluginBuilderAttribute
		private boolean logMdc = true;

		@Override
		public StructuredLoggingLayout build() {
			StructuredLoggingFormat format = createFormat();
			return new StructuredLoggingLayout(format, this.pid, this.serviceName, this.serviceVersion,
					this.serviceNodeName, this.serviceEnvironment, this.logMdc);
		}

		private StructuredLoggingFormat createFormat() {
			StructuredLoggingFormat commonFormat = CommonStructuredLoggingFormats.get(this.format);
			if (commonFormat != null) {
				return commonFormat;
			}
			else if (ClassUtils.isPresent(this.format, null)) {
				return BeanUtils.instantiateClass(ClassUtils.resolveClassName(this.format, null),
						StructuredLoggingFormat.class);
			}
			else {
				throw new IllegalArgumentException("Unknown format '%s'. Common formats are: %s".formatted(this.format,
						CommonStructuredLoggingFormats.getSupportedFormats()));
			}
		}

	}

}
