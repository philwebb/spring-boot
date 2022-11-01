/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.startup;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.SerializerFactory;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.context.metrics.buffering.StartupTimeline;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * {@link Endpoint @Endpoint} to expose the timeline of the
 * {@link org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup
 * application startup}.
 *
 * @author Brian Clozel
 * @author Chris Bono
 * @since 2.4.0
 */
@Endpoint(id = "startup")
public class StartupEndpoint {

	private final BufferingApplicationStartup applicationStartup;

	/**
	 * Creates a new {@code StartupEndpoint} that will describe the timeline of buffered
	 * application startup events.
	 * @param applicationStartup the application startup
	 */
	public StartupEndpoint(BufferingApplicationStartup applicationStartup) {
		this.applicationStartup = applicationStartup;
	}

	@ReadOperation
	public StartupResponse startupSnapshot() {
		StartupTimeline startupTimeline = this.applicationStartup.getBufferedTimeline();
		return new StartupResponse(startupTimeline);
	}

	@WriteOperation
	public StartupResponse startup() {
		StartupTimeline startupTimeline = this.applicationStartup.drainBufferedTimeline();
		return new StartupResponse(startupTimeline);
	}

	/**
	 * A description of an application startup, primarily intended for serialization to
	 * JSON.
	 */
	@JsonSerialize(using = ActuatorJsonSerializer.class)
	public static final class StartupResponse {

		private final String springBootVersion;

		private final StartupTimeline timeline;

		private StartupResponse(StartupTimeline timeline) {
			this.timeline = timeline;
			this.springBootVersion = SpringBootVersion.getVersion();
		}

		public String getSpringBootVersion() {
			return this.springBootVersion;
		}

		public StartupTimeline getTimeline() {
			return this.timeline;
		}

	}

	static class ActuatorJsonSerializer extends JsonSerializer<Object> {

		private static final ObjectMapper objectMapper;

		static {
			Jackson2ObjectMapperBuilder objectMapperBuilder = new Jackson2ObjectMapperBuilder() {

				@Override
				public void configure(ObjectMapper objectMapper) {
					objectMapper.setSerializerFactory(new AnnotationIgnoringBeanSerializerFactory(null));
					super.configure(objectMapper);
				}

			};
			objectMapper = objectMapperBuilder.build();
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			objectMapper.writer().writeValue(gen, value);
		}

		static class AnnotationIgnoringBeanSerializerFactory extends BeanSerializerFactory {

			protected AnnotationIgnoringBeanSerializerFactory(SerializerFactoryConfig config) {
				super(config);
			}

			@Override
			public SerializerFactory withConfig(SerializerFactoryConfig config) {
				return (getFactoryConfig() != config) ? new AnnotationIgnoringBeanSerializerFactory(config) : this;
			}

			@Override
			protected JsonSerializer<Object> findSerializerFromAnnotation(SerializerProvider prov, Annotated a)
					throws JsonMappingException {
				return null;
			}

		}

	}

}
