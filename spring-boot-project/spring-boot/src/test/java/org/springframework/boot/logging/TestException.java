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

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author pwebb
 */
public class TestException {

	public static Exception create() {
		AtomicReference<Exception> exception = new AtomicReference<>();
		Thread thread = new Thread(() -> exception.set(createTestException()));
		thread.start();
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		return exception.get();
	}

	private static Exception createTestException() {
		Throwable root = new RuntimeException("root");
		Throwable cause = createCause(root);
		Exception exception = createException(cause);
		exception.addSuppressed(new RuntimeException("supressed"));
		return exception;
	}

	private static Throwable createCause(Throwable root) {
		return new RuntimeException("cause", root);
	}

	private static Exception createException(Throwable cause) {
		return actualCreateException(cause);
	}

	private static Exception actualCreateException(Throwable cause) {
		return new RuntimeException("exception", cause);
	}

}
