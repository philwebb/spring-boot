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

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * {@link StackTracePrinter} that prints a stack trace in a standard form. This printer
 * can produce a result in a similar form to {@link Throwable#printStackTrace()}, but
 * offers many more customization options.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class StandardStackTracePrinter implements StackTracePrinter {

	private static final StackTraceElement[] NO_ELEMENTS = {};

	private static final String CAUSE_CAPTION = "Caused by: ";

	private static final String SUPPRESSED_CAPTION = "Suppressed: ";

	@Override
	public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		printEnclosedStackTrace(throwable, out, NO_ELEMENTS, "", "", seen);
	}

	private void printEnclosedStackTrace(Throwable throwable, Appendable out, StackTraceElement[] enclosingTrace,
			String caption, String prefix, Set<Throwable> seen) throws IOException {
		if (seen.add(throwable)) {
			StackTraceElement[] trace = throwable.getStackTrace();
			int traceCount = trace.length - 1;
			int enclosingTraceCount = enclosingTrace.length - 1;
			while (traceCount >= 0 && enclosingTraceCount >= 0
					&& trace[traceCount].equals(enclosingTrace[enclosingTraceCount])) {
				traceCount--;
				enclosingTraceCount--;
			}
			int framesInCommon = trace.length - 1 - traceCount;
			extracted2(throwable, out, prefix, caption, seen, trace, traceCount, framesInCommon);
		}
		else {
			out.append(prefix + caption + "[CIRCULAR REFERENCE: " + throwable + "]");
			out.append("\n");
		}
	}

	private void extracted2(Throwable throwable, Appendable out, String prefix, String caption, Set<Throwable> seen,
			StackTraceElement[] trace, int traceCount, int framesInCommon) throws IOException {
		println(out, prefix + caption + throwable);
		for (int i = 0; i <= traceCount; i++) {
			println(out, prefix + "\tat " + trace[i]);
		}
		if (framesInCommon != 0) {
			println(out, prefix + "\t... " + framesInCommon + " more");
		}
		for (Throwable se : throwable.getSuppressed()) {
			printEnclosedStackTrace(se, out, trace, SUPPRESSED_CAPTION, prefix + "\t", seen);
		}
		Throwable ourCause = throwable.getCause();
		if (ourCause != null) {
			printEnclosedStackTrace(ourCause, out, trace, CAUSE_CAPTION, prefix, seen);
		}
	}

	private void println(Appendable out, String string) throws IOException {
		out.append(string);
		out.append("\n");
	}

	public StandardStackTracePrinter withMaximumLength(int maximumLength) {
		return this;
	}

	public StandardStackTracePrinter withMaximumThrowableDepth(int maximumThrowableDepth) {
		return this;
	}

	public StandardStackTracePrinter withFilter(Predicate<Throwable> predicate) {
		return this;
	}

	public StandardStackTracePrinter withElementFilter(Predicate<StackTraceElement> predicate) {
		return this;
	}

	public StandardStackTracePrinter withClassNameFormatter(UnaryOperator<String> classNameFormatter) {
		return this;
	}

	// Printer?

	public StandardStackTracePrinter with() {
		return this;
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception last (the same as {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root last
	 */
	public static StandardStackTracePrinter rootLast() {
		return new StandardStackTracePrinter();
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception first (the opposite of {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root first
	 */
	public static StandardStackTracePrinter rootFirst() {
		return new StandardStackTracePrinter();
	}

}
