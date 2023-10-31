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
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.ehcache.shadow.org.terracotta.utilities.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

	@TempDir
	Path temp;

	private final CertificateFile certificateFile = mock(CertificateFile.class);

	private Path emptyFile;

	@BeforeEach
	void setup() throws Exception {
		this.emptyFile = Files.createFile(this.temp.resolve("empty"));
	}

	@Test
	void forBundlesWithNamesLimitsToBundleNames() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.of((certificateFile, candidates) -> candidates.get(0))
			.forBundles("a", "b");
		List<PrivateKeyFile> candidates = List.of(mock(PrivateKeyFile.class));
		assertThat(selector.selectPrivateKeyFile(this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("a", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("b", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("c", this.certificateFile, candidates)).isNull();
	}

	@Test
	void forBundlesWithPredicateLimitsToBundles() {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.of((certificateFile, candidates) -> candidates.get(0))
			.forBundles((name) -> name.startsWith("a"));
		List<PrivateKeyFile> candidates = List.of(mock(PrivateKeyFile.class));
		assertThat(selector.selectPrivateKeyFile(this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("aa", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("ab", this.certificateFile, candidates)).isEqualTo(candidates.get(0));
		assertThat(selector.selectPrivateKeyFile("b", this.certificateFile, candidates)).isNull();
	}

	@Test
	void usingFileNameWhenHasSingleMatchReturnsFile() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingFileName();
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("c.key"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingFileNameWhenHasMultipleMatchesThrowsException() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingFileName();
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("b.pkf"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("/b.key")
			.withMessageContaining("/b.pkf");
	}

	@Test
	void usingUniquePathMatchWhenHasSingleMatchReturnsFile() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniquePathMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("c.key"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingUniquePathMatchNameWhenHasMultipleMatchesThrowsException() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniquePathMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("b.pkf"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("/b.key")
			.withMessageContaining("/b.pkf");
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenSingleMatchReturnsPrivateKey(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKey> candidatePrivateKeys = new ArrayList<>(source.nonMatchingPrivateKeys());
		candidatePrivateKeys.add(source.privateKey());
		List<PrivateKeyFile> candidates = candidatePrivateKeys.stream()
			.map((privateKey) -> new PrivateKeyFile(this.emptyFile, privateKey))
			.toList();
		CertificateFile certificateFile = new CertificateFile(this.emptyFile, List.of(source.matchingCertificate()));
		assertThat(selector.selectPrivateKeyFile("bundle", certificateFile, candidates))
			.isEqualTo(candidates.get(candidates.size() - 1));
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenMultipleMatchThrowsException(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKey> candidatePrivateKeys = new ArrayList<>(source.nonMatchingPrivateKeys());
		candidatePrivateKeys.add(source.privateKey());
		candidatePrivateKeys.add(source.privateKey());
		List<PrivateKeyFile> candidates = candidatePrivateKeys.stream()
			.map((privateKey) -> new PrivateKeyFile(this.emptyFile, privateKey))
			.toList();
		CertificateFile certificateFile = new CertificateFile(this.emptyFile, List.of(source.matchingCertificate()));
		assertThatIllegalStateException()
			.isThrownBy(() -> selector.selectPrivateKeyFile("bundle", certificateFile, candidates))
			.withMessageContaining("Unable to select PrivateKey due to multiple matches");
	}

	@CertificateMatchingTest
	void usingCertificateMatchWhenNoneMatchReturnsNull(CertificateMatchingTestSource source) {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingCertificateMatch();
		List<PrivateKeyFile> candidates = source.nonMatchingPrivateKeys()
			.stream()
			.map((privateKey) -> new PrivateKeyFile(this.emptyFile, privateKey))
			.toList();
		CertificateFile certificateFile = new CertificateFile(this.emptyFile, List.of(source.matchingCertificate()));
		assertThat(selector.selectPrivateKeyFile("bundle", certificateFile, candidates)).isNull();
	}

	@Test
	void usingUniqueMatchWhenPredicateIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PrivateKeyFileSelector.usingUniqueMatch(null))
			.withMessage("Predicate must not be null");
	}

	@Test
	void usingUniqueMatchWhenNoMatchesReturnsNull() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch((certificatePath,
				privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("spring"));
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("c.key"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isNull();
	}

	@Test
	void usingUniqueMatchWhenSingleMatchReturnsFile() throws IOException {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("c.key"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThat(selector.selectPrivateKeyFile(certificateFile, candidates)).isSameAs(candidates.get(1));
	}

	@Test
	void usingUniqueMatchWhenMultipleMatchesThrowsException() throws Exception {
		PrivateKeyFileSelector selector = PrivateKeyFileSelector.usingUniqueMatch(
				(certificatePath, privateKeyPath) -> privateKeyPath.path().getFileName().toString().startsWith("b"));
		List<PrivateKeyFile> candidates = List.of(createPrivateKeyFile("a.key"), createPrivateKeyFile("b.key"),
				createPrivateKeyFile("b.pkf"));
		CertificateFile certificateFile = createCertificateFile("b.crt");
		assertThatIllegalStateException().isThrownBy(() -> selector.selectPrivateKeyFile(certificateFile, candidates))
			.withMessageContaining("Unable to select")
			.withMessageContaining("/b.key")
			.withMessageContaining("/b.pkf");
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

	private PrivateKeyFile createPrivateKeyFile(String filename) throws IOException {
		Path path = this.temp.resolve(filename);
		Files.createFile(path);
		PrivateKey privateKey = mock(PrivateKey.class);
		return new PrivateKeyFile(path, privateKey);
	}

	private CertificateFile createCertificateFile(String filename) throws IOException {
		Path path = this.temp.resolve(filename);
		Files.createFile(path);
		X509Certificate certificate = mock(X509Certificate.class);
		return new CertificateFile(path, List.of(certificate));
	}

}
