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

package org.springframework.boot.logging.logback;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.json.CommonJsonFormats;
import org.springframework.boot.logging.json.JsonBuilder;
import org.springframework.boot.logging.json.JsonFormat;
import org.springframework.boot.logging.json.LogEvent;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link Encoder Logback encoder} which encodes to JSON-based formats.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class JsonEncoder extends EncoderBase<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

	private JsonFormat format;

	private Long pid;

	private String serviceName;

	private String serviceVersion;

	private String serviceNodeName;

	private String serviceEnvironment;

	public JsonEncoder() {
		// Constructor needed for Logback XML configuration
		this(null);
	}

	public JsonEncoder(JsonFormat format) {
		this(format, null, null, null, null, null);
	}

	public JsonEncoder(JsonFormat format, Long pid, String serviceName, String serviceVersion, String serviceNodeName,
			String serviceEnvironment) {
		this.format = format;
		this.pid = pid;
		this.serviceName = serviceName;
		this.serviceVersion = serviceVersion;
		this.serviceNodeName = serviceNodeName;
		this.serviceEnvironment = serviceEnvironment;
	}

	/**
	 * Sets the format. Accepts either a common format ID, or a fully qualified class
	 * name.
	 * @param format the format
	 */
	public void setFormat(String format) {
		JsonFormat commonFormat = CommonJsonFormats.get(format);
		if (commonFormat != null) {
			this.format = commonFormat;
		}
		else if (ClassUtils.isPresent(format, null)) {
			this.format = BeanUtils.instantiateClass(ClassUtils.resolveClassName(format, null), JsonFormat.class);
		}
		else {
			throw new IllegalArgumentException("Unknown format '%s'. Supported common formats are: %s".formatted(format,
					CommonJsonFormats.getSupportedFormats()));
		}
	}

	public void setPid(Long pid) {
		this.pid = pid;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setServiceVersion(String serviceVersion) {
		this.serviceVersion = serviceVersion;
	}

	public void setServiceNodeName(String serviceNodeName) {
		this.serviceNodeName = serviceNodeName;
	}

	public void setServiceEnvironment(String serviceEnvironment) {
		this.serviceEnvironment = serviceEnvironment;
	}

	@Override
	public void start() {
		Assert.state(this.format != null, "Format has not been set");
		super.start();
		this.throwableProxyConverter.start();
	}

	@Override
	public void stop() {
		this.throwableProxyConverter.stop();
		super.stop();
	}

	@Override
	public byte[] headerBytes() {
		return null;
	}

	@Override
	public byte[] encode(ILoggingEvent event) {
		JsonBuilder jsonBuilder = new JsonBuilder();
		jsonBuilder.objectStart();
		this.format.write(new LogbackLogEventAdapter(event), jsonBuilder);
		jsonBuilder.objectEnd();
		jsonBuilder.newLine();
		return jsonBuilder.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public byte[] footerBytes() {
		return null;
	}

	/**
	 * Adapts an {@link ILoggingEvent} to an {@link LogEvent}.
	 */
	private final class LogbackLogEventAdapter implements LogEvent {

		private final ILoggingEvent event;

		LogbackLogEventAdapter(ILoggingEvent event) {
			this.event = event;
		}

		@Override
		public Instant getTimestamp() {
			return this.event.getInstant();
		}

		@Override
		public String getLevel() {
			return this.event.getLevel().toString();
		}

		@Override
		public Long getPid() {
			return JsonEncoder.this.pid;
		}

		@Override
		public String getThreadName() {
			return this.event.getThreadName();
		}

		@Override
		public String getServiceName() {
			return JsonEncoder.this.serviceName;
		}

		@Override
		public String getServiceVersion() {
			return JsonEncoder.this.serviceVersion;
		}

		@Override
		public String getServiceEnvironment() {
			return JsonEncoder.this.serviceEnvironment;
		}

		@Override
		public String getServiceNodeName() {
			return JsonEncoder.this.serviceNodeName;
		}

		@Override
		public String getLoggerName() {
			return this.event.getLoggerName();
		}

		@Override
		public String getFormattedMessage() {
			return this.event.getFormattedMessage();
		}

		@Override
		public boolean hasThrowable() {
			return this.event.getThrowableProxy() != null;
		}

		@Override
		public String getThrowableClassName() {
			return this.event.getThrowableProxy().getClassName();
		}

		@Override
		public String getThrowableMessage() {
			return this.event.getThrowableProxy().getMessage();
		}

		@Override
		public String getThrowableStackTraceAsString() {
			return JsonEncoder.this.throwableProxyConverter.convert(this.event);
		}

		@Override
		public Map<String, Object> getKeyValuePairs() {
			if (CollectionUtils.isEmpty(this.event.getKeyValuePairs())) {
				return Collections.emptyMap();
			}
			Map<String, Object> result = new HashMap<>();
			for (KeyValuePair keyValuePair : this.event.getKeyValuePairs()) {
				result.put(keyValuePair.key, keyValuePair.value);
			}
			return result;
		}

		@Override
		public Map<String, String> getMdc() {
			return this.event.getMDCPropertyMap();
		}

		@Override
		public int getLevelValue() {
			return this.event.getLevel().toInt();
		}

		@Override
		public Set<String> getMarkers() {
			Set<String> result = new HashSet<>();
			for (Marker marker : this.event.getMarkerList()) {
				addMarker(result, marker);
			}
			return result;
		}

		private void addMarker(Set<String> result, Marker marker) {
			result.add(marker.getName());
			if (marker.hasReferences()) {
				Iterator<Marker> iterator = marker.iterator();
				while (iterator.hasNext()) {
					Marker reference = iterator.next();
					addMarker(result, reference);
				}
			}
		}

	}

}
