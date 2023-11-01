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

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link CertificateFileSelector}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CertificateFileSelectorTests {

	private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

	private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

	@TempDir
	private Path temp;

	@Test
	void forBundlesWithNamesLimitsToBundleNames() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forBundles("a", "b");
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW,
				(files) -> files.add("test").withValidityOffsets(-10, 10));
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("a", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("b", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("c", candidates)).isNull();
	}

	@Test
	void forBundlesWithPredicateLimitsToBundles() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forBundles((name) -> name.startsWith("a"));
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW,
				(files) -> files.add("test").withValidityOffsets(-10, 10));
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("aa", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("ab", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("b", candidates)).isNull();
	}

	@Test
	void forInDateCertificatesLimitsToInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forInDateLeafCertificates(FIXED_CLOCK);
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("startsInFuture").withValidityOffsets(1, 15);
			files.add("expired").withValidityOffsets(-10, -1);
			files.add("valid").withValidityOffsets(-10, 10);
		});
		CertificateFile expected = candidates.get(2);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingFileCreationTimeSelectsNewestFile() {
		CertificateFileSelector selector = CertificateFileSelector.usingFileCreationTime();
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-1, 1);
			files.add("b").withValidityOffsets(-1, 1).withCreationTimeOffset(+10);
			files.add("c").withValidityOffsets(-1, 1).withCreationTimeOffset(+5);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingLeafCertificateValidityPeriodStartSelectsBasedOnNotBeforeField() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodStart();
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-30, 10);
			files.add("b").withValidityOffsets(-10, 10);
			files.add("c").withValidityOffsets(-20, 10);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingLeafCertificateValidityPeriodEndSelectsBasedOnNotAfterField() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodEnd();
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-10, 10);
			files.add("b").withValidityOffsets(-10, 30);
			files.add("c").withValidityOffsets(-10, 20);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingLeafCertificateFieldSelectsBasedOnExtractedField() {
		CertificateFileSelector selector = CertificateFileSelector
			.usingLeafCertificateField(X509Certificate::getNotAfter);
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-10, 10);
			files.add("b").withValidityOffsets(-10, 30);
			files.add("c").withValidityOffsets(-10, 20);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingWithExtractorSelectsBasedOnExtracted() {
		CertificateFileSelector selector = CertificateFileSelector
			.using((candidate) -> candidate.leafCertificate().getNotAfter());
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-10, 10);
			files.add("b").withValidityOffsets(-10, 30);
			files.add("c").withValidityOffsets(-10, 20);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void usingWithComparatorSelectsBasedOnCompared() {
		CertificateFileSelector selector = CertificateFileSelector
			.using(Comparator.comparing((candidate) -> candidate.leafCertificate().getNotAfter()));
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("a").withValidityOffsets(-10, 10);
			files.add("b").withValidityOffsets(-10, 30);
			files.add("c").withValidityOffsets(-10, 20);
		});
		CertificateFile expected = candidates.get(1);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(expected);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(expected);
	}

	@Test
	void ofWhenSelectorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> CertificateFileSelector.of(null))
			.withMessage("Selector must not be null");
	}

	@Test
	void ofReturnsSelector() {
		CertificateFileSelector selector = (candidates) -> candidates.get(0);
		assertThat(CertificateFileSelector.of(selector)).isSameAs(selector);
	}

	void usingLeafCertificateValidityPeriodStartForInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodStart()
			.forInDateLeafCertificates(FIXED_CLOCK);
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("validNotBeforeMinus10").withValidityOffsets(-10, 1);
			files.add("notValidStartsInFuture").withValidityOffsets(1, 15);
			files.add("notValidExpired").withValidityOffsets(-10, -1);
			files.add("validNotBeforeMinus20").withValidityOffsets(-20, 1);
		});
		CertificateFile expected = candidates.get(0);
		CertificateFile selected = selector.selectCertificateFile(candidates);
		assertThat(selected).isEqualTo(expected);
	}

	@Test
	void usingLeafCertificateValidityPeriodEndForInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodEnd()
			.forInDateLeafCertificates(FIXED_CLOCK);
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("validNotAfter10Sec").withValidityOffsets(-10, 10);
			files.add("notValidStartsInFuture").withValidityOffsets(1, 15);
			files.add("notValidExpired").withValidityOffsets(-10, -1);
			files.add("validNotAfter20Sec").withValidityOffsets(-10, 20);
		});
		CertificateFile expected = candidates.get(3);
		CertificateFile selected = selector.selectCertificateFile(candidates);
		assertThat(selected).isEqualTo(expected);
	}

	@Test
	void usingFileCreationTimeForInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.usingFileCreationTime()
			.forInDateLeafCertificates(FIXED_CLOCK);
		List<CertificateFile> candidates = MockCertificateFiles.create(NOW, (files) -> {
			files.add("valid").withValidityOffsets(-10, 10);
			files.add("notValidStartsInFuture").withValidityOffsets(1, 15).withCreationTimeOffset(+10);
			files.add("notValidExpired").withValidityOffsets(-1, -1).withCreationTimeOffset(+20);
			files.add("validNewer").withValidityOffsets(-1, 1).withCreationTimeOffset(+30);
		});
		CertificateFile expected = candidates.get(3);
		CertificateFile selected = selector.selectCertificateFile(candidates);
		assertThat(selected).isEqualTo(expected);
	}

}
