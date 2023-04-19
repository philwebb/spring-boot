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

package org.springframework.boot.ssl.certificate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemCertificateParser}.
 *
 * @author Scott Frederick
 */
class CertificateParserTests {

	@Test
	void parseCertificate() throws Exception {
		X509Certificate[] certificates = PemCertificateParser.parse(fromResource("classpath:test-cert.pem"));
		assertThat(certificates).isNotNull();
		assertThat(certificates).hasSize(1);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
	}

	@Test
	void parseCertificateChain() throws Exception {
		X509Certificate[] certificates = PemCertificateParser.parse(fromResource("classpath:test-cert-chain.pem"));
		assertThat(certificates).isNotNull();
		assertThat(certificates).hasSize(2);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
		assertThat(certificates[1].getType()).isEqualTo("X.509");
	}

	private String fromResource(String resource) throws Exception {
		URL url = ResourceUtils.getURL(resource);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			return FileCopyUtils.copyToString(reader);
		}
	}

}
