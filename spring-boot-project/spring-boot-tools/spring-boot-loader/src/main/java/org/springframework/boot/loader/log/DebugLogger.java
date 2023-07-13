/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.log;

/**
 * Simple logger class used for debugging.
 *
 * @author Phillip Webb
 */
public abstract sealed class DebugLogger {

	private static final String ENABLED_PROPERTY = "org.springframework.boot.loader.debug";

	private static final DebugLogger disabled;
	static {
		disabled = Boolean.getBoolean(ENABLED_PROPERTY) ? null : new NoOpDebugLogger();
	}

	public abstract void log(String message);

	public abstract void log(String message, Object arg1);

	public abstract void log(String message, Object arg1, Object arg2);

	public abstract void log(String message, Object arg1, Object arg2, Object arg3);

	public abstract void log(String message, Object arg1, Object arg2, Object arg3, Object arg4);

	public static DebugLogger get(Class<?> sourceClass) {
		return (disabled != null) ? disabled : new SystemDebugLogger(sourceClass);
	}

	private static final class NoOpDebugLogger extends DebugLogger {

		@Override
		public void log(String message) {
		}

		@Override
		public void log(String message, Object arg1) {
		}

		@Override
		public void log(String message, Object arg1, Object arg2) {
		}

		@Override
		public void log(String message, Object arg1, Object arg2, Object arg3) {
		}

		@Override
		public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
		}

	}

	private static final class SystemDebugLogger extends DebugLogger {

		private final String prefix;

		SystemDebugLogger(Class<?> sourceClass) {
			this.prefix = "LOADER: " + sourceClass + " : ";
		}

		@Override
		public void log(String message) {
			print(message);
		}

		@Override
		public void log(String message, Object arg1) {
			print(message.formatted(arg1));
		}

		@Override
		public void log(String message, Object arg1, Object arg2) {
			print(message.formatted(arg1, arg2));
		}

		@Override
		public void log(String message, Object arg1, Object arg2, Object arg3) {
			print(message.formatted(arg1, arg2, arg3));
		}

		@Override
		public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
			print(message.formatted(arg1, arg2, arg3, arg4));
		}

		private void print(String message) {
			System.err.println(this.prefix + message);
		}

	}

}
