/*
 * Copyright 2012-2023 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.MDC;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Initializer for SLF4J's {@link MDC mapped diagnostic context}.
 *
 * @author Phillip Webb
 */
class MappedDiagnosticContextInitializer {

	private static final String APPLICATION_CORRELATION_IDENTIFIER_NAME = "applicationCorrelationId";

	private static final String APPLICATION_CORRELATION_IDENTIFIER_PROPERTY = "spring.application.correlation-id";

	private static final String APPLICATION_NAME_PROPERTY = "spring.application.name";

	private final BiConsumer<String, String> mdc;

	MappedDiagnosticContextInitializer() {
		this(MDC::put);
	}

	MappedDiagnosticContextInitializer(BiConsumer<String, String> mdc) {
		this.mdc = mdc;
	}

	/**
	 * Initialize the {@link MDC mapped diagnostic context}.
	 * @param loggingInitializationContext the logging initialization context
	 */
	void initialize(LoggingInitializationContext loggingInitializationContext) {
		Environment environment = loggingInitializationContext.getEnvironment();
		Iterable<ConfigurationPropertySource> sources = ConfigurationPropertySources.get(environment);
		CorrelationPropertySourcesPlaceholdersResolver placeholdersResolver = new CorrelationPropertySourcesPlaceholdersResolver(
				environment);
		Binder binder = new Binder(sources, placeholdersResolver, null, null, null);
		MdcPutProperties put = binder.bind("logging.mdc.put", MdcPutProperties.class).orElse(MdcPutProperties.DEFAULTS);
		if (put.applicationCorrelationId()) {
			String applicationCorrelationId = environment.getProperty(APPLICATION_CORRELATION_IDENTIFIER_PROPERTY);
			applicationCorrelationId = (applicationCorrelationId != null) ? applicationCorrelationId
					: placeholdersResolver.resolvePlaceholder(APPLICATION_CORRELATION_IDENTIFIER_PROPERTY);
			if (StringUtils.hasText(applicationCorrelationId)) {
				this.mdc.accept(APPLICATION_CORRELATION_IDENTIFIER_NAME, applicationCorrelationId);
			}
		}
		put.properties().forEach(this.mdc::accept);
	}

	/**
	 * Record for MDC put configuration properties.
	 *
	 * @param applicationCorrelationId if the application correlation ID should be put
	 * @param properties any additional properties to put
	 */
	static record MdcPutProperties(boolean applicationCorrelationId, @DefaultValue Map<String, String> properties) {

		static final MdcPutProperties DEFAULTS = new MdcPutProperties(true, Collections.emptyMap());

	}

	/**
	 * {@link PropertySourcesPlaceholdersResolver} supporting generation of application
	 * correlation identifiers.
	 */
	private static class CorrelationPropertySourcesPlaceholdersResolver extends PropertySourcesPlaceholdersResolver {

		private final Environment environment;

		private final Map<String, String> hashes = new HashMap<>();

		public CorrelationPropertySourcesPlaceholdersResolver(Environment environment) {
			super(environment);
			this.environment = environment;
		}

		@Override
		protected String resolvePlaceholder(String placeholder) {
			String result = super.resolvePlaceholder(placeholder);
			if (result != null) {
				return result;
			}
			if (APPLICATION_CORRELATION_IDENTIFIER_PROPERTY.equals(placeholder)) {
				String applicationName = this.environment.getProperty(APPLICATION_NAME_PROPERTY);
				if (!StringUtils.hasText(applicationName)) {
					return null;
				}
				return this.hashes.computeIfAbsent(applicationName, this::computeHash);
			}
			return null;
		}

		private String computeHash(String input) {
			try {
				MessageDigest digest = MessageDigest.getInstance("SHA-256");
				byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
				String hex = HexFormat.of().formatHex(bytes);
				return hex.substring(0, 10);
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
