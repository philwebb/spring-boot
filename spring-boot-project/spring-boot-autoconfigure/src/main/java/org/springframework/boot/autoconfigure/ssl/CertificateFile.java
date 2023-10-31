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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A certificate file that contains {@link X509Certificate X509 certificates}.
 *
 * @param path the path of the file that contains the content
 * @param certificates the certificates contained in the file
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.2.0
 */
public record CertificateFile(Path path, List<X509Certificate> certificates) {

	public CertificateFile {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(Files.isRegularFile(path), "Path '%s' must be a regular file".formatted(path));
		Assert.isTrue(!CollectionUtils.isEmpty(certificates), "Certificates must not be empty");
	}

	/**
	 * Return the leaf certificate which by convention is the first element in
	 * {@link #certificates()}.
	 * @return the primary certificate
	 */
	public X509Certificate leafCertificate() {
		return certificates().get(0);
	}

	@Override
	public String toString() {
		return "'" + this.path + "'";
	}

	/**
	 * Load a new {@link CertificateFile} from the given PEM file.
	 * @param path the path of the PEM file
	 * @return a new {@link CertificateFile} instance
	 * @throws IOException on IO error
	 */
	static CertificateFile loadFromPemFile(Path path) throws IOException {
		try {
			List<X509Certificate> certificates = PemContent.load(path).getCertificates();
			return new CertificateFile(path, certificates);
		}
		catch (IllegalStateException ex) {
			throw new IllegalStateException("Cannot load certificates from PEM file '%s'".formatted(path));
		}
	}

}
