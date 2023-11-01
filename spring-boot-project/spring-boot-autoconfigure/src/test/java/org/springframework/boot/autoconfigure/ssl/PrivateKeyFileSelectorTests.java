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

import java.security.PrivateKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrivateKeyFileSelector}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class PrivateKeyFileSelectorTests {

	private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

	private final CertificateFile certificateFile = mock(CertificateFile.class);

	@Test
	void forBundlesWithNamesLimitsToBundleNames() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.of((certificateFile, candidates) -> candidates.get(0))
			.forBundles("a", "b");
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> files.add("a"));
		assertThat(selector.selectPrivateKeyFile(this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("a", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("b", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("c", this.certificateFile, candidates)).isNull();
	}

	@Test
	void forBundlesWithPredicateLimitsToBundles() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.of((certificateFile, candidates) -> candidates.get(0))
			.forBundles((name) -> name.startsWith("a"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> files.add("a"));
		assertThat(selector.selectPrivateKeyFile(this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("aa", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("ab", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("b", this.certificateFile, candidates)).isNull();
	}

	@Test
	void usingFileNameWhenHasSingleMatchReturnsFile() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingFileName();
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("c.key");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingFileNameWhenHasMultipleMatchesThrowsException() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingFileName();
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("b.pkf");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("b.key")
			.withMessageContaining("b.pkf");
	}

	@Test
	void usingUniquePathMatchWhenHasSingleMatchReturnsFile() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniquePathMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("c.key");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingUniquePathMatchNameWhenHasMultipleMatchesThrowsException() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniquePathMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("b.pkf");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("b.key")
			.withMessageContaining("b.pkf");
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenSingleMatchReturnsPrivateKey(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKey> privateKeys = new ArrayList<>(source.nonMatchingPrivateKeys());
		privateKeys.add(source.privateKey());
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.createFromPrivateKeys(NOW, privateKeys);
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "cert.crt",
				source.matchingCertificate());
		assertThat(selector.selectPrivateKeyFile("bundle", certificateFile, candidates))
			.isEqualTo(candidates.get(candidates.size() - 1));
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenMultipleMatchThrowsException(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKey> privateKeys = new ArrayList<>(source.nonMatchingPrivateKeys());
		privateKeys.add(source.privateKey());
		privateKeys.add(source.privateKey());
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.createFromPrivateKeys(NOW, privateKeys);
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "cert.crt",
				source.matchingCertificate());
		assertThatIllegalStateException()
			.isThrownBy(() -> selector.selectPrivateKeyFile("bundle", certificateFile, candidates))
			.withMessageContaining("Unable to select PrivateKey due to multiple matches");
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenNoneMatchReturnsNull(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.createFromPrivateKeys(NOW,
				source.nonMatchingPrivateKeys());
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "cert.crt",
				source.matchingCertificate());
		assertThat(selector.selectPrivateKeyFile("bundle", certificateFile, candidates)).isNull();
	}

	@Test
	void usingUniqueMatchWhenPredicateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PrivateKeyFileSelector.usingUniqueMatch(null))
			.withMessage("Predicate must not be null");
	}

	@Test
	void usingUniqueMatchWhenNoMatchesReturnsNull() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch((certificatePath,
				privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("spring"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("c.key");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isNull();
	}

	@Test
	void usingUniqueMatchWhenSingleMatchReturnsFile() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("c.key");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingUniqueMatchWhenMultipleMatchesThrowsException() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = MockPrivateKeyFiles.create(NOW, (files) -> {
			files.add("a.key");
			files.add("b.key");
			files.add("b.pkf");
		});
		CertificateFile certificateFile = MockCertificateFiles.createSingle(NOW, "b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("b.key")
			.withMessageContaining("b.pkf");
	}

	@Test
	void ofWhenSelectorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PrivateKeyFileSelector.of(null))
			.withMessage("Selector must not be null");
	}

	@Test
	void ofReturnsSelector() {
		PrivateKeyFileSelector selector = (certificateFile, candidates) -> candidates.get(0);
		assertThat(PrivateKeyFileSelector.of(selector)).isSameAs(selector);
	}

}
