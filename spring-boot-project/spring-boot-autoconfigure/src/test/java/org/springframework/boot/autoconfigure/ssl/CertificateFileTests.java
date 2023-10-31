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
import java.util.Collections;
import java.util.List;

import org.ehcache.shadow.org.terracotta.utilities.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CertificateFile}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CertificateFileTests {

	private Path pemFile;

	private List<X509Certificate> certificates;

	@TempDir
	Path temp;

	@BeforeEach
	void setup() throws Exception {
		this.pemFile = new ClassPathResource("rsa-cert.pem", getClass()).getFile().toPath();
		this.certificates = PemContent.load(this.pemFile).getCertificates();
	}

	@Test
	void createWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CertificateFile(null, this.certificates))
			.withMessage("Path must not be null");
	}

	@Test
	void createWhenPathIsNotRegularFileThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CertificateFile(this.temp, this.certificates))
			.withMessageContaining("must be a regular file")
			.withMessageContaining(this.temp.toString());
	}

	@Test
	void createWhenCertificatesIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CertificateFile(this.pemFile, null))
			.withMessage("Certificates must not be empty");
	}

	@Test
	void createWhenCertificatesIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CertificateFile(this.pemFile, Collections.emptyList()))
			.withMessage("Certificates must not be empty");
	}

	@Test
	void firstCertificateReturnsFirstCertificate() {
		List<X509Certificate> certificates = List.of(this.certificates.get(0), mock(X509Certificate.class));
		CertificateFile certificateFile = new CertificateFile(this.pemFile, certificates);
		assertThat(certificateFile.leafCertificate()).isEqualTo(certificates.get(0));
	}

	@Test
	void toStringReturnsPath() {
		CertificateFile certificateFile = new CertificateFile(this.pemFile, this.certificates);
		assertThat(certificateFile).hasToString("'" + this.pemFile.toString() + "'");
	}

	@Test
	void loadFromPemFileWhenNoCertificatesThrowsException() throws Exception {
		Path file = this.temp.resolve("empty");
		Files.createFile(file);
		assertThatIllegalStateException().isThrownBy(() -> CertificateFile.loadFromPemFile(file))
			.withMessageContaining("Cannot load certificates from PEM file")
			.withMessageContaining(file.toString());
	}

	@Test
	void loadFromPemFileLoadsContent() throws Exception {
		CertificateFile certificateFile = CertificateFile.loadFromPemFile(this.pemFile);
		assertThat(certificateFile.certificates()).isEqualTo(this.certificates);
	}

}
