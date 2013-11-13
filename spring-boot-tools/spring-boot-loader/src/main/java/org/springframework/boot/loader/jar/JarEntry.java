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

import java.security.CodeSigner;
import java.security.cert.Certificate;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link JarFile}s.
 * 
 * @author Phillip Webb
 */
class JarEntry extends java.util.jar.JarEntry {

	private final JarEntryData source;

	public JarEntry(JarEntryData source) {
		super(source.getName().toString());
		this.source = source;
	}

	public JarEntryData getSource() {
		return this.source;
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
