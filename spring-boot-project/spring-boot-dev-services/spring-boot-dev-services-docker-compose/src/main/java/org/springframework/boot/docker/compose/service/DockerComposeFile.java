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

package org.springframework.boot.docker.compose.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public final class DockerComposeFile {

	private static final List<String> SEARCH_ORDER = List.of("compose.yaml", "compose.yml", "docker-compose.yaml",
			"docker-compose.yml");

	private DockerComposeFile(File file) {
	}

	public static DockerComposeFile find(File workingDirectory) {
		Assert.notNull(workingDirectory, "WorkingDirectory must not be null");
		Path basePath = workingDirectory.toPath();
		for (String candidate : SEARCH_ORDER) {
			Path resolved = basePath.resolve(candidate);
			if (Files.exists(resolved)) {
				return of(resolved.toAbsolutePath().toFile());
			}
		}
		return null;
	}

	public static DockerComposeFile of(File file) {
		Assert.notNull(file, "File must not be null");
		Assert.state(file.exists(), () -> "'%s' does not exist".formatted(file));
		Assert.state(file.isFile(), () -> "'%s' is not a file".formatted(file));
		return new DockerComposeFile(file);
	}

}
