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

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CertificateFileSelector}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CertificateFileSelectorTests {

	private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

	private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.of("UTC"));

	@Test
	void forBundlesWithNamesLimitsToBundleNames() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forBundles("a", "b");
		List<CertificateFile> candidates = List.of(createCertificateFile("test", -10, 10));
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("a", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("b", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("c", candidates)).isNull();
	}

	@Test
	void forBundlesWithPredicateLimitsToBundles() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forBundles((name) -> name.startsWith("a"));
		List<CertificateFile> candidates = List.of(createCertificateFile("test", -10, 10));
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("aa", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("ab", candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectCertificateFile("b", candidates)).isNull();
	}

	@Test
	void forInDateCertificatesLimitsToInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.of((candidates) -> candidates.get(0))
			.forInDateLeafCertificates(FIXED_CLOCK);
		CertificateFile startsInFuture = createCertificateFile("a", 1, 15);
		CertificateFile expired = createCertificateFile("b", -10, -1);
		CertificateFile valid = createCertificateFile("c", -10, 10);
		List<CertificateFile> candidates = List.of(startsInFuture, expired, valid);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(valid);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(valid);
	}

	@Test
	void usingFileCreationTimeSelectsNewestFile() throws Exception {
		CertificateFileSelector selector = CertificateFileSelector.usingFileCreationTime();
		CertificateFile c1 = createCertificateFile(mockPath("a", NOW), -1, 1);
		CertificateFile c2 = createCertificateFile(mockPath("b", NOW.plusSeconds(10)), -1, 1);
		CertificateFile c3 = createCertificateFile(mockPath("c", NOW.plusSeconds(5)), -1, 1);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
	}

	@Test
	void usingLeafCertificateValidityPeriodStartSelectsBasedOnNotBeforeField() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodStart();
		CertificateFile c1 = createCertificateFile("a", -30, 10);
		CertificateFile c2 = createCertificateFile("a", -10, 10);
		CertificateFile c3 = createCertificateFile("a", -20, 10);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
	}

	@Test
	void usingLeafCertificateValidityPeriodEndSelectsBasedOnNotAfterField() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodEnd();
		CertificateFile c1 = createCertificateFile("a", -10, 10);
		CertificateFile c2 = createCertificateFile("a", -10, 30);
		CertificateFile c3 = createCertificateFile("a", -10, 20);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
	}

	@Test
	void usingLeafCertificateFieldSelectsBasedOnExtractedField() {
		CertificateFileSelector selector = CertificateFileSelector
			.usingLeafCertificateField(X509Certificate::getNotAfter);
		CertificateFile c1 = createCertificateFile("a", -10, 10);
		CertificateFile c2 = createCertificateFile("a", -10, 30);
		CertificateFile c3 = createCertificateFile("a", -10, 20);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
	}

	@Test
	void usingWithExtractorSelectsBasedOnExtracted() {
		CertificateFileSelector selector = CertificateFileSelector
			.using((candidate) -> candidate.firstCertificate().getNotAfter());
		CertificateFile c1 = createCertificateFile("a", -10, 10);
		CertificateFile c2 = createCertificateFile("a", -10, 30);
		CertificateFile c3 = createCertificateFile("a", -10, 20);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
	}

	@Test
	void usingWithComparatorSelectsBasedOnCompared() {
		CertificateFileSelector selector = CertificateFileSelector
			.using(Comparator.comparing((candidate) -> candidate.firstCertificate().getNotAfter()));
		CertificateFile c1 = createCertificateFile("a", -10, 10);
		CertificateFile c2 = createCertificateFile("a", -10, 30);
		CertificateFile c3 = createCertificateFile("a", -10, 20);
		List<CertificateFile> candidates = List.of(c1, c2, c3);
		assertThat(selector.selectCertificateFile(candidates)).isEqualTo(c2);
		assertThat(selector.selectCertificateFile("name", candidates)).isEqualTo(c2);
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
		CertificateFile validNotBeforeMinus10 = createCertificateFile("a", -10, 1);
		CertificateFile notValidStartsInFuture = createCertificateFile("b", 1, 15);
		CertificateFile notValidExpired = createCertificateFile("c", -10, -1);
		CertificateFile validNotBeforeMinus20 = createCertificateFile("e", -20, 1);
		CertificateFile selected = selector.selectCertificateFile(
				List.of(validNotBeforeMinus10, notValidStartsInFuture, notValidExpired, validNotBeforeMinus20));
		assertThat(selected).isEqualTo(validNotBeforeMinus10);
	}

	@Test
	void usingLeafCertificateValidityPeriodEndForInDateCertificates() {
		CertificateFileSelector selector = CertificateFileSelector.usingLeafCertificateValidityPeriodEnd()
			.forInDateLeafCertificates(FIXED_CLOCK);
		CertificateFile validNotAfter10Sec = createCertificateFile("a", -10, 10);
		CertificateFile notValidStartsInFuture = createCertificateFile("b", 1, 15);
		CertificateFile notValidExpired = createCertificateFile("c", -10, -1);
		CertificateFile validNotAfter20Sec = createCertificateFile("d", -10, 20);
		CertificateFile selected = selector.selectCertificateFile(
				List.of(validNotAfter10Sec, notValidStartsInFuture, notValidExpired, validNotAfter20Sec));
		assertThat(selected).isEqualTo(validNotAfter20Sec);
	}

	@Test
	void usingFileCreationTimeForInDateCertificates() throws Exception {
		CertificateFileSelector selector = CertificateFileSelector.usingFileCreationTime()
			.forInDateLeafCertificates(FIXED_CLOCK);
		Path p1 = mockPath("a", NOW);
		Path p2 = mockPath("b", NOW.plusSeconds(10));
		Path p3 = mockPath("c", NOW.plusSeconds(20));
		Path p4 = mockPath("d", NOW.plusSeconds(30));
		CertificateFile valid = createCertificateFile(p1, -10, 10);
		CertificateFile notValidStartsInFuture = createCertificateFile(p2, 1, 15);
		CertificateFile notValidExpired = createCertificateFile(p3, -1, -1);
		CertificateFile validNewer = createCertificateFile(p4, -1, 1);
		CertificateFile selected = selector
			.selectCertificateFile(List.of(valid, notValidStartsInFuture, notValidExpired, validNewer));
		assertThat(selected).isEqualTo(validNewer);
	}

	private Path mockPath(String name, Instant creationTime) throws IOException {
		Path path = mock(Path.class);
		FileSystem fileSystem = mock(FileSystem.class);
		FileSystemProvider provider = mock(FileSystemProvider.class);
		BasicFileAttributes attributes = mock(BasicFileAttributes.class);
		given(path.getFileSystem()).willReturn(fileSystem);
		given(fileSystem.provider()).willReturn(provider);
		given(provider.readAttributes(path, BasicFileAttributes.class)).willReturn(attributes);
		given(attributes.creationTime()).willReturn(FileTime.from(creationTime));
		return path;
	}

	private CertificateFile createCertificateFile(String path, int notBeforeDelta, int notAfterDelta) {
		return createCertificateFile(Path.of(path), notBeforeDelta, notAfterDelta);
	}

	private CertificateFile createCertificateFile(Path path, int notBeforeDelta, int notAfterDelta) {
		return new CertificateFile(path, createCertificateChain(notBeforeDelta, notAfterDelta));
	}

	private List<X509Certificate> createCertificateChain(int notBeforeDelta, int notAfterDelta) {
		return List.of(createCertificate(notBeforeDelta, notAfterDelta));
	}

	private X509Certificate createCertificate(int notBeforeDelta, int notAfterDelta) {
		return createCertificate(NOW.plusSeconds(notBeforeDelta), NOW.plusSeconds(notAfterDelta));
	}

	private X509Certificate createCertificate(Instant notBefore, Instant notAfter) {
		X509Certificate certificate = Mockito.mock(X509Certificate.class);
		given(certificate.getNotBefore()).willReturn(java.util.Date.from(notBefore));
		given(certificate.getNotAfter()).willReturn(java.util.Date.from(notAfter));
		return certificate;
	}

}
