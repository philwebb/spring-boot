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
import java.security.PrivateKey;

import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.util.Assert;

/**
 * {@link PrivateKey} content loaded from a file.
 *
 * @param path the path of the file that contains the content
 * @param privateKey the parsed private key
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.2.0
 */
public record PrivateKeyFile(Path path, PrivateKey privateKey) {

	public PrivateKeyFile {
		Assert.notNull(path, "Path must not be null");
		Assert.isTrue(Files.isRegularFile(path), "Path '%s' must be a regular file".formatted(path));
		Assert.isTrue(privateKey != null, "PrivateKey must not be null");
	}

	@Override
	public String toString() {
		return "'" + this.path + "'";
	}

	/**
	 * Load a new {@link PrivateKeyFile} from the given PEM file.
	 * @param path the path of the PEM file
	 * @param privateKeyPassword the private key password or {@code null}
	 * @return a new {@link PrivateKeyFile} instance
	 * @throws IOException on IO error
	 */
	static PrivateKeyFile loadFromPemFile(Path path, String privateKeyPassword) throws IOException {
		try {
			PrivateKey privateKey = PemContent.load(path).getPrivateKey(privateKeyPassword);
			return new PrivateKeyFile(path, privateKey);
		}
		catch (IllegalStateException ex) {
			throw new IllegalStateException("Cannot load private key from PEM file '%s'".formatted(path));
		}
	}

}
