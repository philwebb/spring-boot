/*
 * Copyright 2018 the original author or authors.
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

package org.junit.rules;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.matcher.AssertionMatcher;
import org.hamcrest.Matcher;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author pwebb
 */
public class MyExpectedException {

	public void expect(Class<? extends Throwable> exceptionClass,
			ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(exceptionClass).isThrownBy(throwingCallable);
	}

	public void expect(Class<? extends Throwable> exceptionClass, String message,
			ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(exceptionClass).isThrownBy(throwingCallable)
				.withMessageContaining(message);
	}

	public void expectMessage(String message) {
	}

	public void expectMessage(Matcher<String> matcher) {
	}

	public void expectCause(Matcher<? extends Throwable> equalTo) {
	}

	public void expect(Matcher<?> equalTo) {
	}

	public void expect(AssertionMatcher<?> assertionMatcher) {
	}

	public static MyExpectedException none() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
