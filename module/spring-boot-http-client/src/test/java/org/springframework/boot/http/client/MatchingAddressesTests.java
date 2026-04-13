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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MatchingAddresses}.
 *
 * @author Phillip Webb
 */
class MatchingAddressesTests {

	@Test
	void toListOrElseThrowWhenNotEmptyReturnsResult() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThat(matching.toList((address) -> true).orElseThrow("localhost", mock())).containsExactly("127.0.0.1");
	}

	@Test
	void toListOrElseThrowWhenEmptyThrowsException() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> matching.toList((address) -> false).orElseThrow("localhost", mock()))
			.withMessage("Unmatched host 'localhost'");
	}

	@Test
	void toArrayOrElseThrowWhenNotEmptyReturnsResult() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThat(matching.toArray((address) -> true, String[]::new).orElseThrow("localhost", mock()))
			.containsExactly("127.0.0.1");
	}

	@Test
	void toArrayOrElseThrowWhenEmptyThrowsException() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> matching.toArray((address) -> false, String[]::new).orElseThrow("localhost", mock()))
			.withMessage("Unmatched host 'localhost'");
	}

	@Test
	void getOrElseThrowWhenNotEmptyReturnsResult() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThat(matching.get((address) -> true).orElseThrow("localhost", mock())).isEqualTo("127.0.0.1");
	}

	@Test
	void getOrElseThrowWhenEmptyThrowsException() {
		MatchingAddresses<String> matching = MatchingAddresses.of(Stream.of("127.0.0.1"));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> matching.get((address) -> false).orElseThrow("localhost", mock()))
			.withMessage("Unmatched host 'localhost'");
	}

}
