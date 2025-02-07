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
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		printStackTrace(seen, new Print(out, "", ""), throwable, NO_ELEMENTS);
	}

	private void printStackTrace(Set<Throwable> seen, Print print, Throwable throwable,
			StackTraceElement[] enclosingElements) throws IOException {
		if (throwable == null) {
			return;
		}
		if (!seen.add(throwable)) {
			print.circularReference(throwable);
			return;
		}
		StackTraceElement[] elements = throwable.getStackTrace();
		Throwable cause = throwable.getCause();
		if (this.rootFirst) {
			printStackTrace(seen, print, cause, elements);
			extracted(seen, print.withWrappedByCaption(cause), throwable, enclosingElements, elements);
		}
		else {
			extracted(seen, print, throwable, enclosingElements, elements);
			printStackTrace(seen, print.withCausedByCaption(cause), cause, elements);
		}
	}

	private void extracted(Set<Throwable> seen, Print print, Throwable throwable, StackTraceElement[] enclosingElements,
			StackTraceElement[] elements) throws IOException {
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
		print.exceptionDetails(throwable);
		for (int i = 0; i <= elementsCount; i++) {
			print.at(elements[i]);
		}
		if (framesInCommon != 0) {
			print.filtered(framesInCommon + " more");
		}
		if (!this.hideSupressed) {
			for (Throwable suppressed : throwable.getSuppressed()) {
				printStackTrace(seen, print.withSuppressed(), suppressed, elements);
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

	private static final class Print {

		private final Appendable out;

		private final String indent;

		private final String caption;

		Print(Appendable out, String indent, String caption) {
			this.out = out;
			this.indent = indent;
			this.caption = caption;
		}

		void circularReference(Throwable throwable) throws IOException {
			println(this.caption + "[CIRCULAR REFERENCE: " + throwable + "]");
		}

		void exceptionDetails(Throwable throwable) throws IOException {
			println(this.caption + throwable);
		}

		void at(StackTraceElement element) throws IOException {
			println("\tat " + element);
		}

		void filtered(String msg) throws IOException {
			println("\t... " + msg);
		}

		private void println(String string) throws IOException {
			this.out.append(this.indent);
			this.out.append(string);
			this.out.append("\n");
		}

		Print withCausedByCaption(Throwable causedBy) {
			return new Print(this.out, this.indent, "Caused by: ");
		}

		Print withWrappedByCaption(Throwable wrappedBy) {
			return (wrappedBy != null) ? new Print(this.out, this.indent, "Wrapped by: ") : this;
		}

		public Print withSuppressed() {
			return new Print(this.out, this.indent + "\t", "Suppressed: ");
		}

	}

	@FunctionalInterface
	private interface Action {

		void run() throws IOException;

	}

}
