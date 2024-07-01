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

package smoketest.simple;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import org.springframework.boot.logging.structured.ApplicationMetadata;
import org.springframework.boot.logging.structured.StructuredLoggingFormatter;

/**
 * Custom {@link StructuredLoggingFormatter}.
 *
 * @author Moritz Halbritter
 */
public class CustomStructuredLoggingFormatter implements StructuredLoggingFormatter<ILoggingEvent> {

	private final ThrowableProxyConverter throwableProxyConverter;

	private final ApplicationMetadata metadata;

	public CustomStructuredLoggingFormatter(ApplicationMetadata metadata,
			ThrowableProxyConverter throwableProxyConverter) {
		this.metadata = metadata;
		this.throwableProxyConverter = throwableProxyConverter;
	}

	@Override
	public String format(ILoggingEvent event) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("epoch=").append(event.getInstant().toEpochMilli());
		if (this.metadata.pid() != null) {
			stringBuilder.append(" pid=").append(this.metadata.pid());
		}
		stringBuilder.append(" msg=\"").append(event.getFormattedMessage()).append('"');
		IThrowableProxy throwable = event.getThrowableProxy();
		if (throwable != null) {
			stringBuilder.append(" error=\"").append(this.throwableProxyConverter.convert(event)).append('"');
		}
		stringBuilder.append('\n');
		return stringBuilder.toString();
	}

}
