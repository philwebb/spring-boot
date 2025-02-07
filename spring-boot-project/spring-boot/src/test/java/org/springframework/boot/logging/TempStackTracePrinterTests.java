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

package org.springframework.boot.logging;

import java.util.List;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.LifeCycle;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter;
import org.springframework.boot.logging.temp.ShortenedThrowableConverter;

/**
 * Tests for {@link StackTracePrinter}
 *
 * @author pwebb
 */
class TempStackTracePrinterTests {

	@Test
	void logback() {
		Throwable ex = createException();
		ch.qos.logback.classic.spi.ThrowableProxy proxy = new ch.qos.logback.classic.spi.ThrowableProxy(ex);
		LoggingEvent event = new LoggingEvent();
		event.setThrowableProxy(proxy);
		ExtendedWhitespaceThrowableProxyConverter converter = new ExtendedWhitespaceThrowableProxyConverter();
		converter.start();
		System.out.println("Logback");
		System.out.println(converter.convert(event));
	}

	@Test
	void logbackref() {
		Throwable ex = createException();
		ch.qos.logback.classic.spi.ThrowableProxy proxy = new ch.qos.logback.classic.spi.ThrowableProxy(ex);
		LoggingEvent event = new LoggingEvent();
		event.setThrowableProxy(proxy);
		ShortenedThrowableConverter converter = new ShortenedThrowableConverter();
		converter.setOptionList(List.of("full", "full", "full", "rootFirst", "omitCommonFrames"));
		((LifeCycle) converter.getClassNameAbbreviator()).start();
		converter.start();
		System.out.println("Logbackrev");
		System.out.println(converter.convert(event));
	}

	@Test
	void log4j() {
		Throwable ex = createException();
		org.apache.logging.log4j.core.impl.ThrowableProxy proxy = new org.apache.logging.log4j.core.impl.ThrowableProxy(
				ex);
		Configuration config = null;
		String[] opts = {};
		ExtendedThrowablePatternConverter converter = ExtendedThrowablePatternConverter.newInstance(config, opts);
		Log4jLogEvent event = new Log4jLogEvent("test", null, null, null, null, ex);
		StringBuilder out = new StringBuilder();
		converter.format(event, out);
		System.out.println("Log4j");
		System.out.println(out);
	}

	@Test
	void java() {
		Throwable ex = createException();
		System.out.println("Java");
		ex.printStackTrace(System.out);
	}

	private Throwable createException() {
		return TestException.create();
	}

}
