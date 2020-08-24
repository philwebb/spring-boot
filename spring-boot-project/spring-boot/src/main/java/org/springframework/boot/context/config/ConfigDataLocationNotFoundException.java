/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.Resource;

/**
 * Exception thrown when a config data location cannot be found.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public class ConfigDataLocationNotFoundException extends ConfigDataException {

	private final ConfigDataLocation location;

	public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
		this(location, null);
	}

	public ConfigDataLocationNotFoundException(ConfigDataLocation location, Throwable cause) {
		this(getMessage(location), location);
	}

	public ConfigDataLocationNotFoundException(String message, ConfigDataLocation location) {
		this(message, location, null);
	}

	public ConfigDataLocationNotFoundException(String message, ConfigDataLocation location, Throwable cause) {
		super(message, cause);
		this.location = location;
	}

	public ConfigDataLocation getLocation() {
		return this.location;
	}

	private static String getMessage(ConfigDataLocation location) {
		return "Config data location '" + location + "' does not exist";
	}

	public static void throwIfDoesNotExist(ConfigDataLocation location, Path path) {
		throwIfDoesNotExist(location, Files.exists(path));
	}

	public static void throwIfDoesNotExist(ConfigDataLocation location, File file) {
		throwIfDoesNotExist(location, file.exists());
	}

	public static void throwIfDoesNotExist(ConfigDataLocation location, Resource resource) {
		throwIfDoesNotExist(location, resource.exists());
	}

	private static void throwIfDoesNotExist(ConfigDataLocation location, boolean exists) {
		if (!exists) {
			throw new ConfigDataLocationNotFoundException(location);
		}
	}

}
