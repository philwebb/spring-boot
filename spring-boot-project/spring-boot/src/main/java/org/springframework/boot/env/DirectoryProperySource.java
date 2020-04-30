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

package org.springframework.boot.env;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 * @since 2.3.0
 */
public class DirectoryProperySource extends EnumerablePropertySource<File> implements OriginLookup<String> {

	private static final int MAX_DEPTH = 100;

	private final Map<String, Property> properties;

	private final String[] names;

	private final boolean immutable;

	/**
	 * Create a new {@link DirectoryProperySource} instance.
	 * @param name the name of the property source
	 * @param source the underlying source directory
	 */
	public DirectoryProperySource(String name, File source) {
		this(name, source, true);
	}

	/**
	 * Create a new {@link DirectoryProperySource} instance.
	 * @param name the name of the property source
	 * @param source the underlying source directory
	 */
	private DirectoryProperySource(String name, File source, boolean immutable) {
		super(name, source);
		Assert.isTrue(source.exists(), "Directory '" + source + "' does not exist");
		Assert.isTrue(source.isDirectory(), "File '" + source + "' is not a directory");
		this.properties = buildProperties(source.toPath());
		this.names = StringUtils.toStringArray(this.properties.keySet());
		this.immutable = immutable;
	}

	private Map<String, Property> buildProperties(Path source) {
		try {
			Map<String, Property> properties = new TreeMap<>();
			Files.find(source, MAX_DEPTH, this::isRegularFile).forEach((file) -> {
				String name = getName(source.relativize(file));
				if (StringUtils.hasText(name)) {
					properties.put(name, new Property(file.toFile()));
				}
			});
			return Collections.unmodifiableMap(properties);
		}
		catch (IOException ex) {
			throw new UnsupportedOperationException("Auto-generated method stub", ex);
		}
	}

	private String getName(Path relativePath) {
		if (relativePath.getNameCount() == 1) {
			return relativePath.toString();
		}
		return null;
	}

	private boolean isRegularFile(Path path, BasicFileAttributes attributes) {
		return attributes.isRegularFile();
	}

	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.getOrDefault(name, Property.NONE).getValue();
	}

	@Override
	public Origin getOrigin(String name) {
		return this.properties.getOrDefault(name, Property.NONE).getOrigin();
	}

	@Override
	public boolean isImmutable() {
		return this.immutable;
	}

	private static final class Property {

		public static final Property NONE = new Property(null);

		private static final Location ZERO_LOCATION = new Location(0, 0);

		private final FileSystemResource resource;

		public Property(File file) {
			this.resource = (file != null) ? new FileSystemResource(file) : null;
		}

		public String getValue() {
			if (this.resource == null) {
				return null;
			}
			try {
				return readContent();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private String readContent() throws FileNotFoundException, IOException {
			FileInputStream in = new FileInputStream(this.resource.getFile());
			InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			return FileCopyUtils.copyToString(reader);
		}

		public Origin getOrigin() {
			if (this.resource == null) {
				return null;
			}
			return new TextResourceOrigin(this.resource, ZERO_LOCATION);
		}

	}

}
