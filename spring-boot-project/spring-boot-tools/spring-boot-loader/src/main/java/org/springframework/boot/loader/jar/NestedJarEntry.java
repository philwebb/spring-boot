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

import java.io.IOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * An individual entry from a {@link NestedJarFile}.
 */
class NestedJarEntry extends java.util.jar.JarEntry {

	private final ZipContent.Entry contentEntry;

	private final String name;

	NestedJarEntry(ZipContent.Entry contentEntry, String name) {
		super(contentEntry.getName());
		this.contentEntry = contentEntry;
		this.name = name;
	}

	ZipContent.Entry contentEntry() {
		return this.contentEntry;
	}

	@Override
	public Attributes getAttributes() throws IOException {
		// Manifest manifest = getManifest();
		// return (manifest != null) ? manifest.getAttributes(getName()) : null;
		return null;
	}

	@Override
	public Certificate[] getCertificates() {
		return getCertification().getCertificates();
	}

	@Override
	public CodeSigner[] getCodeSigners() {
		return getCertification().getCodeSigners();
	}

	private NestedJarEntryCertification getCertification() {
		// @formatter:off
//			if (!this.jarFile.isSigned()) {
//				return JarEntryCertification.NONE;
//			}
//			JarEntryCertification certification = this.certification;
//			if (certification == null) {
//				certification = this.jarFile.getCertification(this);
//				this.certification = certification;
//			}
//			return certification;
			// @formatter:on
		return null;
	}

	@Override
	public String getRealName() {
		return super.getName();
	}

	@Override
	public String getName() {
		return this.name;
	}

}
