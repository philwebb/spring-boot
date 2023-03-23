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
import java.security.PrivateKey;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 */
class PrivateKeyParserTests {

	@Test
	void parsePkcs8KeyFile() throws Exception {
		PrivateKey privateKey = PrivateKeyParser.parse(fromResource("classpath:test-key.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
	}

	@Test
	void parsePkcs8KeyFileWithEcdsa() throws Exception {
		PrivateKey privateKey = PrivateKeyParser.parse(fromResource("classpath:test-ec-key.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void parseWithNonKeyTextWillThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> PrivateKeyParser.parse(fromResource("classpath:test-banner.txt")));
	}

	private String fromResource(String resource) throws Exception {
		URL url = ResourceUtils.getURL(resource);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			return FileCopyUtils.copyToString(reader);
		}
	}

}
