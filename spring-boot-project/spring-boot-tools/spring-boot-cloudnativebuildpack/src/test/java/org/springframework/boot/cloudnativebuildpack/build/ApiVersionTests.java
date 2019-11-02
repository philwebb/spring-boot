/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.cloudnativebuildpack.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApiVersion}.
 *
 * @author Phillip Webb
 */
class ApiVersionTests {

	@Test
	void parseWhenVersionIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse(null))
				.withMessage("Value must not be empty");
	}

	@Test
	void parseWhenVersionIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse(""))
				.withMessage("Value must not be empty");
	}

	@Test
	void parseWhenVersionDoesNotMatchPatternThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ApiVersion.parse("bad"))
				.withMessage("Malformed version number 'bad'");
	}

	@Test
	void parseReturnsVersion() {
		ApiVersion version = ApiVersion.parse("1.2");
		assertThat(version.getMajor()).isEqualTo(1);
		assertThat(version.getMinor()).isEqualTo(2);
	}

	@Test
	void assertSupportsWhenSupports() {
		ApiVersion.parse("1.2").assertSupports(ApiVersion.parse("1.0"));
	}

	@Test
	void assertSupportsWhenDoesNotSupportThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> ApiVersion.parse("1.2").assertSupports(ApiVersion.parse("1.3")))
				.withMessage("Version 'v1.3' is not supported by this version ('v1.2')");
	}

	@Test
	void supportWhenSame() {
		assertThat(supports("0.0", "0.0")).isTrue();
		assertThat(supports("0.1", "0.1")).isTrue();
		assertThat(supports("1.0", "1.0")).isTrue();
		assertThat(supports("1.1", "1.1")).isTrue();
	}

	@Test
	void supportsWhenDifferentMajor() {
		assertThat(supports("0.0", "1.0")).isFalse();
		assertThat(supports("1.0", "0.0")).isFalse();
		assertThat(supports("1.0", "2.0")).isFalse();
		assertThat(supports("2.0", "1.0")).isFalse();
		assertThat(supports("1.1", "2.1")).isFalse();
		assertThat(supports("2.1", "1.1")).isFalse();
	}

	@Test
	void supportsWhenDifferentMinor() {
		assertThat(supports("1.2", "1.1")).isTrue();
		assertThat(supports("1.2", "1.3")).isFalse();
	}

	@Test
	void supportWhenMajorZeroAndDifferentMinor() {
		assertThat(supports("0.2", "0.1")).isFalse();
		assertThat(supports("0.2", "0.3")).isFalse();
	}

	@Test
	void toStringReturnsString() {
		assertThat(ApiVersion.parse("1.2").toString()).isEqualTo("v1.2");
	}

	@Test
	void equalsAndHashCode() {
		ApiVersion v12a = ApiVersion.parse("1.2");
		ApiVersion v12b = ApiVersion.parse("1.2");
		ApiVersion v13 = ApiVersion.parse("1.3");
		assertThat(v12a.hashCode()).isEqualTo(v12b.hashCode());
		assertThat(v12a).isEqualTo(v12a).isEqualTo(v12b).isNotEqualTo(v13);
	}

	private boolean supports(String v1, String v2) {
		return ApiVersion.parse(v1).supports(ApiVersion.parse(v2));
	}

}
