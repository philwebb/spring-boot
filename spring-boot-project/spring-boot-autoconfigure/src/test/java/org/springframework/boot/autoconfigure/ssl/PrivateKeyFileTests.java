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
import java.security.PrivateKey;

import org.ehcache.shadow.org.terracotta.utilities.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyFile}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class PrivateKeyFileTests {

	private Path pemFile;

	private PrivateKey privateKey;

	@TempDir
	Path temp;

	@BeforeEach
	void setup() throws Exception {
		this.pemFile = new ClassPathResource("rsa-key.pem", getClass()).getFile().toPath();
		this.privateKey = PemContent.load(this.pemFile).getPrivateKey();
	}

	@Test
	void createWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrivateKeyFile(null, this.privateKey))
			.withMessage("Path must not be null");
	}

	@Test
	void createWhenPathIsNotRegularFileThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrivateKeyFile(this.temp, this.privateKey))
			.withMessageContaining("must be a regular file")
			.withMessageContaining(this.temp.toString());
	}

	@Test
	void createWhenPrivateKeyIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new PrivateKeyFile(this.pemFile, null))
			.withMessage("PrivateKey must not be null");
	}

	@Test
	void toStringReturnsPath() {
		PrivateKeyFile certificateFile = new PrivateKeyFile(this.pemFile, this.privateKey);
		assertThat(certificateFile).hasToString("'" + this.pemFile.toString() + "'");
	}

	@Test
	void loadFromPemFileWhenNoPrivateKeyThrowsException() throws Exception {
		Path file = this.temp.resolve("empty");
		Files.createFile(file);
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyFile.loadFromPemFile(file, null))
			.withMessageContaining("Cannot load private key from PEM file")
			.withMessageContaining(file.toString());
	}

	@Test
	void loadFromPemFileLoadsContent() throws Exception {
		PrivateKeyFile certificateFile = PrivateKeyFile.loadFromPemFile(this.pemFile, null);
		assertThat(certificateFile.privateKey()).isEqualTo(this.privateKey);
	}

}
