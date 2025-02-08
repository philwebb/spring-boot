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

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StandardStackTracePrinter}.
 *
 * @author Phillip Webb
 */
class StandardStackTracePrinterTests {

	@Test
	void rootLastPrintsStackTrace() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo(standardStackTrace());
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
	void withCommonFramesWhenRootLastPrintsAllFrames() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast().withCommonFrames();
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
	void withCommonFramesWhenRootFirstPrintsAllFrames() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst().withCommonFrames();
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

	@Test
	void withoutSuppressedHidesSuppressed() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast().withoutSuppressed();
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
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
	void withMaximumLengthWhenNegativeThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StandardStackTracePrinter.rootFirst().withMaximumLength(0))
			.withMessage("'maximumLength' must be positive");
	}

	@Test
	void withMaximumLengthTruncatesOutput() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst().withMaximumLength(14);
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("java.lang.R...");
	}

	@Test
	void withMaximumThrowableDepthWhenNegativeThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StandardStackTracePrinter.rootFirst().withMaximumThrowableDepth(0))
			.withMessage("'maximumThrowableDepth' must be positive");
	}

	@Test
	void withMaximumThrowableDepthFiltersElements() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst().withMaximumThrowableDepth(1);
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					... 2 more
				Wrapped by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					... 1 filtered
					... 2 more
				Wrapped by: java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					... 4 filtered
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						... 2 more
						""");
	}

	@Test
	void withMaximumThrowableDepthAndCommonFramesFiltersElements() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst()
			.withCommonFrames()
			.withMaximumThrowableDepth(2);
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					... 1 filtered
				Wrapped by: java.lang.RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					... 2 filtered
				Wrapped by: java.lang.RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					... 3 filtered
					Suppressed: java.lang.RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
						... 1 filtered
						""");
	}

	@Test
	void withFilterWhenPredicateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> StandardStackTracePrinter.rootFirst().withFilter(null))
			.withMessage("'predicate' must not be null");
	}

	@Test
	void withFilterWhenFilterMatches() {
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst()
			.withFilter(IllegalStateException.class::isInstance);
		assertThat(printer.printStackTraceToString(new IllegalStateException("test"))).isNotEmpty();
	}

	@Test
	void withFilterWhenFilterDoesNotMatch() {
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst()
			.withFilter(IllegalStateException.class::isInstance);
		assertThat(printer.printStackTraceToString(new RuntimeException("test"))).isEmpty();
	}

	@Test
	void withMultipleFiltersMustAllMatch() {
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst()
			.withFilter(IllegalStateException.class::isInstance)
			.withFilter((ex) -> "test".equals(ex.getMessage()));
		assertThat(printer.printStackTraceToString(new IllegalStateException("test"))).isNotEmpty();
		assertThat(printer.printStackTraceToString(new IllegalStateException("nope"))).isEmpty();
		assertThat(printer.printStackTraceToString(new RuntimeException("test"))).isEmpty();
	}

	@Test
	void withFrameFilter() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootFirst()
			.withCommonFrames()
			.withFrameFilter((index, element) -> element.getMethodName().startsWith("lambda"));
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: root
					... 1 filtered
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					... 1 filtered
				Wrapped by: java.lang.RuntimeException: cause
					... 2 filtered
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					... 1 filtered
				Wrapped by: java.lang.RuntimeException: exception
					... 3 filtered
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					... 1 filtered
					Suppressed: java.lang.RuntimeException: supressed
						... 1 filtered
						at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
						... 1 filtered
						""");
	}

	@Test
	void withEscapedLineSeparatorUsesEscapedLineSeparator() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast().withEscapedLineSeprator();
		assertThat(printer.printStackTraceToString(exception))
			.isEqualTo(standardStackTrace().replace(System.lineSeparator(), "\\n"));
	}

	@Test
	void withLineSeparatorUsesLineSeparator() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast().withLineSeparator("!");
		assertThat(printer.printStackTraceToString(exception))
			.isEqualTo(standardStackTrace().replace(System.lineSeparator(), "!"));
	}

	@Test
	void withFormatterWhenFormatterIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> StandardStackTracePrinter.rootLast().withFormatter(null))
			.withMessage("'formatter' must not be null");
	}

	@Test
	void withFormatterFormatsThrowable() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast()
			.withFormatter((throwable) -> ClassUtils.getShortName(throwable.getClass()) + ": "
					+ throwable.getLocalizedMessage());
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				RuntimeException: exception
					at org.springframework.boot.logging.TestException.actualCreateException(TestException.java:56)
					at org.springframework.boot.logging.TestException.createException(TestException.java:52)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:42)
					at org.springframework.boot.logging.TestException.lambda$0(TestException.java:28)
					at java.base/java.lang.Thread.run(Thread.java:840)
					Suppressed: RuntimeException: supressed
						at org.springframework.boot.logging.TestException.createTestException(TestException.java:43)
						... 2 more
				Caused by: RuntimeException: cause
					at org.springframework.boot.logging.TestException.createCause(TestException.java:48)
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:41)
					... 2 more
				Caused by: RuntimeException: root
					at org.springframework.boot.logging.TestException.createTestException(TestException.java:40)
					... 2 more
					""");
	}

	@Test
	void withFrameFormatterWhenFormatterIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> StandardStackTracePrinter.rootLast().withFrameFormatter(null))
			.withMessage("'frameFormatter' must not be null");
	}

	@Test
	void withFrameFormatterFormatsFrame() {
		Throwable exception = TestException.create();
		StandardStackTracePrinter printer = StandardStackTracePrinter.rootLast()
			.withFrameFormatter(
					(element) -> ClassUtils.getShortName(element.getClassName()) + "." + element.getMethodName());
		assertThat(printer.printStackTraceToString(exception)).isEqualTo("""
				java.lang.RuntimeException: exception
					at TestException.actualCreateException
					at TestException.createException
					at TestException.createTestException
					at TestException.lambda$0
					at Thread.run
					Suppressed: java.lang.RuntimeException: supressed
						at TestException.createTestException
						... 2 more
				Caused by: java.lang.RuntimeException: cause
					at TestException.createCause
					at TestException.createTestException
					... 2 more
				Caused by: java.lang.RuntimeException: root
					at TestException.createTestException
					... 2 more
					""");
	}

	private String standardStackTrace() {
		return """
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
					""";
	}

}
