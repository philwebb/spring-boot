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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.PathResource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 * @since 2.3.0
 */
public class DirectoryPathProperySource extends EnumerablePropertySource<Path> implements OriginLookup<String> {

	private static final int MAX_DEPTH = 100;

	private final Map<String, PropertyFile> propertyFiles;

	private final String[] names;

	private final boolean cacheContent;

	/**
	 * Create a new {@link DirectoryPathProperySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 */
	public DirectoryPathProperySource(String name, Path sourceDirectory) {
		this(name, sourceDirectory, true);
	}

	/**
	 * Create a new {@link DirectoryPathProperySource} instance.
	 * @param name the name of the property source
	 * @param sourceDirectory the underlying source directory
	 */
	private DirectoryPathProperySource(String name, Path sourceDirectory, boolean cacheContent) {
		super(name, sourceDirectory);
		Assert.isTrue(Files.exists(sourceDirectory), "Directory '" + sourceDirectory + "' does not exist");
		Assert.isTrue(Files.isDirectory(sourceDirectory), "File '" + sourceDirectory + "' is not a directory");
		this.propertyFiles = PropertyFile.findAll(sourceDirectory, cacheContent);
		this.cacheContent = cacheContent;
		this.names = StringUtils.toStringArray(this.propertyFiles.keySet());
	}

	@Override
	public String[] getPropertyNames() {
		return this.names.clone();
	}

	@Override
	public Value getProperty(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getContent() : null;
	}

	@Override
	public Origin getOrigin(String name) {
		PropertyFile propertyFile = this.propertyFiles.get(name);
		return (propertyFile != null) ? propertyFile.getOrigin() : null;
	}

	@Override
	public boolean isImmutable() {
		return this.cacheContent;
	}

	public interface Value extends CharSequence, InputStreamSource {

	}

	private static final class PropertyFile {

		private static final Location START_OF_FILE = new Location(0, 0);

		private final PathResource resource;

		private final PropertyFileContent cachedContent;

		private PropertyFile(Path path, boolean cacheContent) {
			this.resource = new PathResource(path);
			this.cachedContent = cacheContent ? new PropertyFileContent(this.resource, true) : null;
		}

		public PropertyFileContent getContent() {
			return (this.cachedContent != null) ? this.cachedContent : new PropertyFileContent(this.resource, false);
		}

		public Origin getOrigin() {
			return new TextResourceOrigin(this.resource, START_OF_FILE);
		}

		public static Map<String, PropertyFile> findAll(Path sourceDirectory, boolean cacheContent) {
			try {
				Map<String, PropertyFile> propertyFiles = new TreeMap<>();
				Files.find(sourceDirectory, MAX_DEPTH, PropertyFile::isRegularFile).forEach((path) -> {
					String name = getName(sourceDirectory.relativize(path));
					if (StringUtils.hasText(name)) {
						propertyFiles.put(name, new PropertyFile(path, cacheContent));
					}
				});
				return Collections.unmodifiableMap(propertyFiles);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to find files in '" + sourceDirectory + "'", ex);
			}
		}

		private static boolean isRegularFile(Path path, BasicFileAttributes attributes) {
			return attributes.isRegularFile();
		}

		private static String getName(Path relativePath) {
			if (relativePath.getNameCount() == 1) {
				return relativePath.toString();
			}
			// FIXME
			return null;
		}

	}

	private static class PropertyFileContent implements Value {

		private final PathResource resource;

		private final boolean cacheContent;

		private volatile byte[] content;

		private PropertyFileContent(PathResource resource, boolean cacheContent) {
			this.resource = resource;
			this.cacheContent = cacheContent;
		}

		@Override
		public int length() {
			return toString().length();
		}

		@Override
		public char charAt(int index) {
			return toString().charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return toString().subSequence(start, end);
		}

		@Override
		public String toString() {
			return new String(getBytes());
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (!this.cacheContent) {
				return this.resource.getInputStream();
			}
			return new ByteArrayInputStream(getBytes());
		}

		private byte[] getBytes() {
			try {
				if (!this.cacheContent) {
					return FileCopyUtils.copyToByteArray(this.resource.getInputStream());
				}
				if (this.content == null) {
					synchronized (this.resource) {
						if (this.content == null) {
							this.content = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
						}
					}
				}
				return this.content;
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

}
