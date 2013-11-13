/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.InputStream;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link JarFile}s.
 * 
 * @author Phillip Webb
 */
class JarEntry extends java.util.jar.JarEntry {

	private String name;

	private final RandomAccessData data;

	private long time = -1;

	public JarEntry(String name, RandomAccessData data) {
		super(name);
		this.name = name;
		this.data = data;
	}

	void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setTime(long time) {
		// Time conversion is slow, only do it on the first read
		this.time = time;
	}

	@Override
	public long getTime() {
		if (this.time != -1) {
			super.setTime(this.time);
			this.time = -1;
		}
		return super.getTime();
	}

	public InputStream getInputStream() {
		InputStream inputStream = getData().getInputStream();
		if (getMethod() == ZipEntry.DEFLATED) {
			inputStream = new ZipInflaterInputStream(inputStream, (int) getSize());
		}
		return inputStream;
	}

	public RandomAccessData getData() {
		return this.data;
	}

	@Override
	public Certificate[] getCertificates() {
		return null; // FIXME;
	}

	@Override
	public CodeSigner[] getCodeSigners() {
		return null; // FIXME
	}

}
