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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StandardStackTracePrinter}.
 *
 * @author Phillip Webb
 */
class StandardStackTracePrinterTests {

	@Test
	void rootLastPrintsStacktrace() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						... 2 more
				Caused by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					... 2 more
				Caused by: java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					... 2 more
					""");
	}

	@Test
	void rootLastPrintsStackTraceThatMatchesJvm() {
		Throwable exception = TestException.create();
		Writer printedJvmStackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(printedJvmStackTrace));
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo(printedJvmStackTrace.toString());
	}

	@Test
	void rootLastWithCommonFramesIncludedPrintsStacktrace() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast().withCommonFramesIncluded();
		System.out.println(printer.printStackTraceToString(exception));
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
						at java.base/java.lang.Thread.run(Thread.java:840)
				Caused by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
				Caused by: java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					""");
	}

	@Test
	void rootFirstPrintsStackTrace() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					... 2 more
				Wrapped by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					... 2 more
				Wrapped by: java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						... 2 more
						""");
	}

	@Test
	void rootFirstWithCommonFramesIncludedPrintsStackTrace() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst().withCommonFramesIncluded();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
				Wrapped by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
				Wrapped by: java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
						at java.base/java.lang.Thread.run(Thread.java:840)
						""");
	}

}
