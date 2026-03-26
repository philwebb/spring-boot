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

import java.io.UncheckedIOException;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * @author Phillip Webb
 */
public class UnmatchedHostException extends UncheckedIOException {

	public UnmatchedHostException(String message) {
		super("", new UnknownHostException(message));
	}

	static <T> Collector<T, ?, List<T>> collectingToList(@Nullable String host, HttpClientInetAddressMatcher matcher,
			Predicate<T> predicate) {
		return collecting(host, matcher, predicate, Collectors.toList());
	}

	static <T, A, R> Collector<T, A, R> collecting(@Nullable String host, HttpClientInetAddressMatcher matcher,
			Predicate<T> predicate, Collector<T, A, R> collector) {
		List<T> unmatched = new ArrayList<>();
		BiConsumer<A, T> accumulator = (a, t) -> {
			if (predicate.test(t)) {
				collector.accumulator().accept(a, t);
			}
			else {
				unmatched.add(t);
			}
		};
		Function<A, R> finisher = (a) -> {
			if (!unmatched.isEmpty()) {
				throw new UnmatchedHostException("Badness");
			}
			return collector.finisher().apply(a);
		};
		return Collector.of(collector.supplier(), accumulator, collector.combiner(), finisher);
	}

}
