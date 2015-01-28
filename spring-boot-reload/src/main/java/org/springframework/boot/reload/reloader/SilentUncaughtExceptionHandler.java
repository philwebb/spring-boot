/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.reload.reloader;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * {@link UncaughtExceptionHandler} decorator that ignores {@link SilentExitException}.
 *
 * @author Phillip Webb
 */
public class SilentUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private final UncaughtExceptionHandler delegate;

	public SilentUncaughtExceptionHandler(UncaughtExceptionHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		if (exception instanceof SilentExitException) {
			return;
		}
		if (this.delegate != null) {
			this.delegate.uncaughtException(thread, exception);
		}
	}

}
