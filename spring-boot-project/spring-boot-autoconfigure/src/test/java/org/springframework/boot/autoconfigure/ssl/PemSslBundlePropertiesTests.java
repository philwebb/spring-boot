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

package org.springframework.boot.autoconfigure.ssl;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties.Store.Select.SelectCertificate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemSslBundleProperties}.
 *
 * @author Phillip Webb
 */
class PemSslBundlePropertiesTests {

	@Nested
	class SelectCertificateTests {

		@Test
		void usingFileCreationTimeSelectsUsingFileCreationTime() {
			testSelect(SelectCertificate.USING_FILE_CREATION_TIME, 2);
		}

		@Test
		void usingValidityPeriodStartSelectsUsingLeafCertificateValidityPeriodStart() {
			testSelect(SelectCertificate.USING_VALIDITY_PERIOD_START, 0);
		}

		@Test
		void usingValidityPeriodEndUsingFileCreationTimeSelectsUsingLeafCertificateValidityPeriodEnd() {
			testSelect(SelectCertificate.USING_VALIDITY_PERIOD_END, 1);
		}

		private void testSelect(SelectCertificate select, int expected) {
			List<CertificateFile> candidates = MockCertificateFiles.create((files) -> {
				files.add("0").withValidityOffsets(-10, 10);
				files.add("1").withValidityOffsets(-20, 20);
				files.add("2").withValidityOffsets(-10, 10).withCreationTimeOffset(100);
				files.add("3").withValidityOffsets(-20, -10).withCreationTimeOffset(200);
			});
			CertificateFile selected = select.getSelector().selectCertificateFile("bundle", candidates);
			assertThat(candidates.indexOf(selected)).isEqualTo(expected);
		}

	}

	@Nested
	class SelectPrivateKeyTests {

	}

}
