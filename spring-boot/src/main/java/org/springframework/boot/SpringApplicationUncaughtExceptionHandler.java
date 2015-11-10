/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.LogFactory;

/**
 * {@link UncaughtExceptionHandler} used to log {@link SpringApplication} run methods.
 *
 * @author Phillip Webb
 */
class SpringApplicationUncaughtExceptionHandler implements UncaughtExceptionHandler {

	private static ThreadLocal<Boolean> attached = new ThreadLocal<Boolean>();

	public SpringApplicationUncaughtExceptionHandler(
			UncaughtExceptionHandler uncaughtExceptionHandler) {
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		LogFactory.getLog(SpringApplication.class).error("Application startup failed", e);
	}

	public static boolean isAttached() {
		return Boolean.TRUE.equals(attached.get());
	}

	public static void attachIfCurrentThreadIsMain() {
		Thread thread = Thread.currentThread();
		if (isMain(thread)) {
			thread.setUncaughtExceptionHandler(
					new SpringApplicationUncaughtExceptionHandler(
							thread.getUncaughtExceptionHandler()));
			attached.set(Boolean.TRUE);
		}
	}

	private static boolean isMain(Thread thread) {
		return "main".equals(thread.getName())
				&& "main".equals(thread.getThreadGroup().getName());
	}

}
