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
import java.util.Arrays;
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
		printFullStackTrace(seen, new Print(out, "", ""), StackTrace.from(throwable), StackTrace.NONE);
	}

	private void printFullStackTrace(Set<Throwable> seen, Print print, StackTrace stackTrace, StackTrace enclosing)
			throws IOException {
		if (stackTrace == null) {
			return;
		}
		if (!seen.add(stackTrace.throwable())) {
			print.circularReference(stackTrace.throwable());
			return;
		}
		StackTrace cause = stackTrace.getCause();
		if (this.rootFirst) {
			printFullStackTrace(seen, print, cause, stackTrace);
			springSingleStackTrace(seen, print.withWrappedByCaption(cause), stackTrace, enclosing);
		}
		else {
			springSingleStackTrace(seen, print, stackTrace, enclosing);
			printFullStackTrace(seen, print.withCausedByCaption(cause), cause, stackTrace);
		}
	}

	private void springSingleStackTrace(Set<Throwable> seen, Print print, StackTrace stackTrace, StackTrace enclosing)
			throws IOException {
		print.thrown(stackTrace.throwable);
		printFrames(print, stackTrace, enclosing);
		if (!this.hideSupressed) {
			for (StackTrace suppressed : stackTrace.getSuppressed()) {
				printFullStackTrace(seen, print.withSuppressedCaption(), suppressed, stackTrace);
			}
		}
	}

	private void printFrames(Print print, StackTrace stackTrace, StackTrace enclosing) throws IOException {
		int framesInCommon = (!this.includeCommonFrames) ? getFramesInCommon(stackTrace, enclosing) : 0;
		for (int i = 0; i < stackTrace.frames.length - framesInCommon; i++) {
			print.at(stackTrace.frames[i]);
		}
		if (framesInCommon != 0) {
			print.omitted(framesInCommon + " more");
		}
	}

	private int getFramesInCommon(StackTrace stackTrace, StackTrace enclosingStackTrace) {
		int elementsCount = stackTrace.frames.length - 1;
		int enclosingElementsCount = enclosingStackTrace.frames.length - 1;
		while (elementsCount >= 0 && enclosingElementsCount >= 0
				&& stackTrace.frames[elementsCount].equals(enclosingStackTrace.frames[enclosingElementsCount])) {
			elementsCount--;
			enclosingElementsCount--;
		}
		int framesInCommon = stackTrace.frames.length - 1 - elementsCount;
		return framesInCommon;
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

		void thrown(Throwable throwable) throws IOException {
			println(this.caption + throwable);
		}

		void at(StackTraceElement element) throws IOException {
			println("\tat " + element);
		}

		void omitted(String message) throws IOException {
			println("\t... " + message);
		}

		private void println(String string) throws IOException {
			this.out.append(this.indent);
			this.out.append(string);
			this.out.append("\n");
		}

		Print withCausedByCaption(StackTrace causedBy) {
			return new Print(this.out, this.indent, "Caused by: ");
		}

		Print withWrappedByCaption(StackTrace wrappedBy) {
			return (wrappedBy != null) ? new Print(this.out, this.indent, "Wrapped by: ") : this;
		}

		public Print withSuppressedCaption() {
			return new Print(this.out, this.indent + "\t", "Suppressed: ");
		}

	}

	record StackTrace(Throwable throwable, StackTraceElement[] frames) {

		static final StackTrace NONE = new StackTrace(null, new StackTraceElement[0]);

		static StackTrace from(Throwable throwable) {
			return (throwable != null) ? new StackTrace(throwable, throwable.getStackTrace()) : null;
		}

		public StackTrace[] getSuppressed() {
			return Arrays.stream(throwable().getSuppressed()).map(StackTrace::from).toArray(StackTrace[]::new);
		}

		public StackTrace getCause() {
			return StackTrace.from(this.throwable.getCause());
		}

	}

	@FunctionalInterface
	private interface Action {

		void run() throws IOException;

	}

}
