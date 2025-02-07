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

	private final boolean rootFirst;

	private final boolean includeCommonFrames; // FIXME enumset at some point

	private final boolean hideSupressed = false;

	private StandardStackTracePrinter(boolean rootFirst, boolean includeCommonFrames) {
		this.rootFirst = rootFirst;
		this.includeCommonFrames = includeCommonFrames;
	}

	@Override
	public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
		Printer printer = new Printer(out, "", "");
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		printStackTrace(printer, seen, throwable, NO_ELEMENTS);
	}

	private void printStackTrace(Printer printer, Set<Throwable> seen, Throwable throwable,
			StackTraceElement[] enclosingElements) throws IOException {
		if (throwable == null) {
			return;
		}
		if (!seen.add(throwable)) {
			printer.circularReference(throwable);
			return;
		}
		StackTraceElement[] elements = throwable.getStackTrace();
		Throwable cause = throwable.getCause();
		if (this.rootFirst) {
			printStackTrace(printer, seen, cause, elements);
			extracted(printer.withCauseCaption(cause, "Wrapped by: "), seen, throwable, enclosingElements, elements);
		}
		else {
			extracted(printer, seen, throwable, enclosingElements, elements);
			printStackTrace(printer.withCauseCaption(cause, "Caused by: "), seen, cause, elements);
		}
	}

	private void extracted(Printer printer, Set<Throwable> seen, Throwable throwable,
			StackTraceElement[] enclosingElements, StackTraceElement[] elements) throws IOException {
		int elementsCount = elements.length - 1;
		int enclosingElementsCount = enclosingElements.length - 1;
		if (!this.includeCommonFrames) {
			while (elementsCount >= 0 && enclosingElementsCount >= 0
					&& elements[elementsCount].equals(enclosingElements[enclosingElementsCount])) {
				elementsCount--;
				enclosingElementsCount--;
			}
		}
		int framesInCommon = elements.length - 1 - elementsCount;
		printer.caption(throwable);
		for (int i = 0; i <= elementsCount; i++) {
			printer.at(elements[i]);
		}
		if (framesInCommon != 0) {
			printer.dotdotdot(framesInCommon + " more");
		}
		if (!this.hideSupressed) {
			for (Throwable suppressed : throwable.getSuppressed()) {
				printStackTrace(printer.withSuppressed(), seen, suppressed, elements);
			}
		}
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

	public StandardStackTracePrinter withCommonFramesIncluded() {
		return new StandardStackTracePrinter(this.rootFirst, true);
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
		return new StandardStackTracePrinter(false, false);
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception first (the opposite of {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root first
	 */
	public static StandardStackTracePrinter rootFirst() {
		return new StandardStackTracePrinter(true, false);
	}

	private static final class Printer {

		private final Appendable out;

		private final String indent;

		private final String caption;

		Printer(Appendable out, String indent, String caption) {
			this.out = out;
			this.indent = indent;
			this.caption = caption;
		}

		void circularReference(Throwable throwable) throws IOException {
			xprintln(this.indent + this.caption + "[CIRCULAR REFERENCE: " + throwable + "]");
		}

		void caption(Throwable throwable) throws IOException {
			xprintln(this.indent + this.caption + throwable);
		}

		void at(StackTraceElement elements) throws IOException {
			xprintln(this.indent + "\tat " + elements);
		}

		void dotdotdot(String msg) throws IOException {
			xprintln(this.indent + "\t... " + msg);
		}

		private void xprintln(String string) throws IOException {
			this.out.append(string);
			this.out.append("\n");
		}

		public Printer withCauseCaption(Throwable cause, String caption) {
			return (cause != null) ? new Printer(this.out, this.indent, caption) : this;
		}

		public Printer withSuppressed() {
			return new Printer(this.out, this.indent + "\t", "Suppressed: ");
		}

		@Override
		public String toString() {
			return this.indent + this.caption;
		}

	}

	enum Prefix3 {

		SUPRESSSED, CAUSED_BY, WRAPPED_BY

	}

	@FunctionalInterface
	private interface Action {

		void run() throws IOException;

	}

}
