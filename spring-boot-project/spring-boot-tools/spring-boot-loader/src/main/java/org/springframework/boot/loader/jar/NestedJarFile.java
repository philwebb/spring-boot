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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 */
class NestedJarFile extends java.util.jar.JarFile {

	NestedJarFile(File containerJar, String nestedEntryName) throws IOException {
		super(containerJar);
		// NestedJarFile(File file, boolean verify, int mode, Runtime.Version version)
		// throws IOException {

		// FIXME needs to deal with directories as well as nested jars
	}

	@Override
	public Manifest getManifest() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Enumeration<JarEntry> entries() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Stream<JarEntry> stream() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public JarEntry getJarEntry(String name) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public ZipEntry getEntry(String name) {
		// FIXME isMultiRelease support
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getComment() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public Stream<JarEntry> versionedStream() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
