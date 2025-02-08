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
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.springframework.util.Assert;

/**
 * {@link StackTracePrinter} that prints a stack trace in a standard form. This printer
 * can produce a result in a similar form to {@link Throwable#printStackTrace()}, but
 * offers many more customization options.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class StandardStackTracePrinter implements StackTracePrinter {

	private static final Predicate<?> ALWAYS = (t) -> true;

	private final EnumSet<Option> options;

	private final Predicate<Throwable> filter;

	private final BiPredicate<Integer, StackTraceElement> elementFilter;

	private StandardStackTracePrinter(EnumSet<Option> options, Predicate<Throwable> filter,
			BiPredicate<Integer, StackTraceElement> elementFilter) {
		this.options = options;
		this.filter = filter;
		this.elementFilter = elementFilter;
	}

	@Override
	public void printStackTrace(Throwable throwable, Appendable out) throws IOException {
		if (this.filter.test(throwable)) {
			Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			printFullStackTrace(seen, new Print(out, "", ""), StackTrace.from(throwable), StackTrace.NONE);
		}
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
		if (!hasOption(Option.ROOT_FIRST)) {
			springSingleStackTrace(seen, print, stackTrace, enclosing);
			printFullStackTrace(seen, print.withCausedByCaption(cause), cause, stackTrace);
		}
		else {
			printFullStackTrace(seen, print, cause, stackTrace);
			springSingleStackTrace(seen, print.withWrappedByCaption(cause), stackTrace, enclosing);
		}
	}

	private void springSingleStackTrace(Set<Throwable> seen, Print print, StackTrace stackTrace, StackTrace enclosing)
			throws IOException {
		print.thrown(stackTrace.throwable);
		printFrames(print, stackTrace, enclosing);
		if (!hasOption(Option.HIDE_SUPPRESSED)) {
			for (StackTrace suppressed : stackTrace.getSuppressed()) {
				printFullStackTrace(seen, print.withSuppressedCaption(), suppressed, stackTrace);
			}
		}
	}

	private void printFrames(Print print, StackTrace stackTrace, StackTrace enclosing) throws IOException {
		int commonFrames = (!hasOption(Option.SHOW_COMMON_FRAMES)) ? stackTrace.commonFramesCount(enclosing) : 0;
		for (int i = 0; i < stackTrace.frames().length - commonFrames; i++) {
			print.at(stackTrace.frames[i]);
		}
		if (commonFrames != 0) {
			print.omitted(commonFrames + " more");
		}
	}

	private boolean hasOption(Option option) {
		return this.options.contains(option);
	}

	public StandardStackTracePrinter withFilter(Predicate<Throwable> predicate) {
		Assert.notNull(predicate, "'predicate' must not be null");
		return new StandardStackTracePrinter(this.options, this.filter.and(predicate));
	}

	public StandardStackTracePrinter withMaximumLength(int maximumLength) {
		// FIXME
		return this;
	}

	public StandardStackTracePrinter withMaximumThrowableDepth(int maximumThrowableDepth) {
		return withElementFilter((index, element) -> index < maximumThrowableDepth);
	}

	public StandardStackTracePrinter withElementFilter(BiPredicate<Integer, StackTraceElement> predicate) {
		// FIXME
		return this;
	}

	public StandardStackTracePrinter withClassNameFormatter(UnaryOperator<String> classNameFormatter) {
		// FIXME
		return this;
	}

	public StandardStackTracePrinter withCommonFrames() {
		return withOption(Option.SHOW_COMMON_FRAMES);
	}

	public StandardStackTracePrinter withoutSuppressed() {
		return withOption(Option.HIDE_SUPPRESSED);
	}

	private StandardStackTracePrinter withOption(Option option) {
		EnumSet<Option> options = EnumSet.copyOf(this.options);
		options.add(option);
		return new StandardStackTracePrinter(options, this.filter);
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception last (the same as {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root last
	 */
	public static StandardStackTracePrinter rootLast() {
		return new StandardStackTracePrinter(EnumSet.noneOf(Option.class), always());
	}

	/**
	 * Return a {@link StandardStackTracePrinter} that prints the stack trace with the
	 * root exception first (the opposite of {@link Throwable#printStackTrace()}).
	 * @return a {@link StandardStackTracePrinter} that prints the stack trace root first
	 */
	public static StandardStackTracePrinter rootFirst() {
		return new StandardStackTracePrinter(EnumSet.of(Option.ROOT_FIRST), always());
	}

	@SuppressWarnings("unchecked")
	private static <T> Predicate<T> always() {
		return (Predicate<T>) ALWAYS;
	}

	private enum Option {

		ROOT_FIRST, SHOW_COMMON_FRAMES, HIDE_SUPPRESSED

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
			line(this.caption + "[CIRCULAR REFERENCE: " + throwable + "]");
		}

		void thrown(Throwable throwable) throws IOException {
			line(this.caption + throwable);
		}

		void at(StackTraceElement element) throws IOException {
			line("\tat " + element);
		}

		void omitted(String message) throws IOException {
			line("\t... " + message);
		}

		private void line(String string) throws IOException {
			this.out.append(this.indent);
			this.out.append(string);
			this.out.append("\n");
		}

		Print withCausedByCaption(StackTrace causedBy) {
			return withCaption(causedBy != null, "", "Caused by: ");
		}

		Print withWrappedByCaption(StackTrace wrappedBy) {
			return withCaption(wrappedBy != null, "", "Wrapped by: ");
		}

		public Print withSuppressedCaption() {
			return withCaption(true, "\t", "Suppressed: ");
		}

		private Print withCaption(boolean test, String extraIndent, String caption) {
			return (test) ? new Print(this.out, this.indent + extraIndent, caption) : this;
		}

	}

	record StackTrace(Throwable throwable, StackTraceElement[] frames) {

		static final StackTrace NONE = new StackTrace(null, new StackTraceElement[0]);

		int commonFramesCount(StackTrace other) {
			int index = this.frames.length - 1;
			int otherIndex = other.frames.length - 1;
			while (index >= 0 && otherIndex >= 0 && this.frames[index].equals(other.frames[otherIndex])) {
				index--;
				otherIndex--;
			}
			return this.frames.length - 1 - index;
		}

		StackTrace[] getSuppressed() {
			return Arrays.stream(throwable().getSuppressed()).map(StackTrace::from).toArray(StackTrace[]::new);
		}

		StackTrace getCause() {
			return StackTrace.from(this.throwable.getCause());
		}

		static StackTrace from(Throwable throwable) {
			return (throwable != null) ? new StackTrace(throwable, throwable.getStackTrace()) : null;
		}
	}

}
