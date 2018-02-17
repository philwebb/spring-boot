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

package org.springframework.boot.convert;

import java.util.Collection;

import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.StringValueResolver;

/**
 * A specialization of {@link DefaultFormattingConversionService} configured by default
 * with converters and formatters appropriate for most Spring Boot applications.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addDefaultFormatters} utility method for ad-hoc use against any
 * {@code FormatterRegistry} instance, just as {@code DefaultConversionService} exposes
 * its own {@link DefaultConversionService#addDefaultConverters addDefaultConverters}
 * method.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationConversionService extends DefaultFormattingConversionService {

	public ApplicationConversionService() {
		this(null, false);
	}

	public ApplicationConversionService(boolean registerDefaultFormatters) {
		this(null, registerDefaultFormatters);
	}

	public ApplicationConversionService(StringValueResolver embeddedValueResolver,
			boolean registerDefaultFormatters) {
		super(embeddedValueResolver, registerDefaultFormatters);
		if (registerDefaultFormatters) {
			addApplicationConverters(this);
			addApplicationFormatters(this);
		}
	}

	public void addApplicationConverters(ConverterRegistry registry) {
		registry.removeConvertible(Object[].class, String.class);
		registry.removeConvertible(String.class, Object[].class);
		registry.removeConvertible(String.class, Collection.class);
		registry.removeConvertible(Collection.class, String.class);
		registry.addConverterFactory(new StringToEnumIgnoringCaseConverterFactory());
	}

	public void addApplicationFormatters(FormatterRegistry registry) {
		registry.addFormatter(new CharArrayFormatter());
		registry.addFormatter(new InetAddressFormatter());
	}

}
