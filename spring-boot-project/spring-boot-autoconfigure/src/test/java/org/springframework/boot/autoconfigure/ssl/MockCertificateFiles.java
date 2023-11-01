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

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.mockito.Mockito;

import org.springframework.util.function.ThrowingFunction;

import static org.mockito.BDDMockito.given;

/**
 * Helper used to build {@link CertificateFile} instances with mock content.
 *
 * @author Phillip Webb
 */
final class MockCertificateFiles {

	private final Instant now;

	private final List<File> files = new ArrayList<>();

	private MockCertificateFiles(Instant now) {
		this.now = now;
	}

	File add(String filename) {
		File file = new File(filename);
		this.files.add(file);
		return file;
	}

	private List<CertificateFile> create() {
		return this.files.stream().map(ThrowingFunction.of(File::create)).toList();
	}

	static CertificateFile createSingle(String filename) {
		return createSingle(Instant.now(), filename, null);
	}

	static CertificateFile createSingle(Instant now, String filename) {
		return createSingle(now, filename, null);
	}

	static CertificateFile createSingle(Instant now, String filename, X509Certificate certificate) {
		return create(now, (files) -> files.add(filename).withCertificate(certificate)).get(0);
	}

	static List<CertificateFile> create(Consumer<MockCertificateFiles> files) {
		return create(Instant.now(), files);
	}

	static List<CertificateFile> create(Instant now, Consumer<MockCertificateFiles> files) {
		MockCertificateFiles mockCertificateFiles = new MockCertificateFiles(now);
		files.accept(mockCertificateFiles);
		return mockCertificateFiles.create();
	}

	final class File {

		private final String name;

		private Instant creationTime;

		private Instant certificateNotBefore;

		private Instant certificateNotAfter;

		private X509Certificate certificate;

		private File(String name) {
			this.name = name;
			this.certificateNotBefore = MockCertificateFiles.this.now;
			this.certificateNotAfter = MockCertificateFiles.this.now;
			this.creationTime = MockCertificateFiles.this.now;
		}

		File withCreationTimeOffset(int creationTimeOffset) {
			this.creationTime = MockCertificateFiles.this.now.plusSeconds(creationTimeOffset);
			return this;
		}

		File withValidityOffsets(int notBeforeOffset, int notAfterOffset) {
			this.certificateNotBefore = MockCertificateFiles.this.now.plusSeconds(notBeforeOffset);
			this.certificateNotAfter = MockCertificateFiles.this.now.plusSeconds(notAfterOffset);
			return this;
		}

		File withCertificate(X509Certificate certificate) {
			this.certificate = certificate;
			return this;
		}

		private CertificateFile create() {
			return new CertificateFile(MockPath.create(this.name, this.creationTime), createCertificates());
		}

		private List<X509Certificate> createCertificates() {
			if (this.certificate != null) {
				return List.of(this.certificate);
			}
			X509Certificate certificate = Mockito.mock(X509Certificate.class);
			given(certificate.getNotBefore()).willReturn(java.util.Date.from(this.certificateNotBefore));
			given(certificate.getNotAfter()).willReturn(java.util.Date.from(this.certificateNotAfter));
			return List.of(certificate);
		}

	}

}
