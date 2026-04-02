/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author pwebb
 */
class MatchingAddresses<T> {

	Stream<T> stream;

	public static <T> MatchingAddresses<T> of(Stream<T> stream) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public Matched<List<T>> toList(Predicate<T> predicate) {
		return null;
	}

	public Matched<T[]> toArray(Predicate<T> predicate, IntFunction<T[]> generator) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public Matched<Void> match(Predicate<T> predicate) {
		return null;
	}

	interface Matched<R> {

		R orElseThrow();

		R orElseThrow(String host);

		R orElseThrow(String host, InetAddressMatcher matcher);

	}

}
