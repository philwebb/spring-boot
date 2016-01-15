/*
 * Copyright 2012-2015 the original author or authors.
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

import java.io.IOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.jar.Attributes;

/**
 * Extended variant of {@link java.util.jar.JarEntry} returned by {@link JarFile}s.
 *
 * @author Phillip Webb
 */
class JarFileEntry extends java.util.jar.JarEntry {

	private Certificate[] certificates;

	private CodeSigner[] codeSigners;

	private long localHeaderOffset;

	JarFileEntry(String name) {
		super(name);
	}

	// /**
	// * Return a {@link URL} for this {@link JarEntry}.
	// * @return the URL for the entry
	// * @throws MalformedURLException if the URL is not valid
	// */
	// URL getUrl() throws MalformedURLException {
	// return new URL(this.source.getSource().getUrl(), getName());
	// }

	@Override
	public Attributes getAttributes() throws IOException {
		// Manifest manifest = this.source.getSource().getManifest();
		// return (manifest == null ? null : manifest.getAttributes(getName()));
		return null; // FIXME
	}

	@Override
	public Certificate[] getCertificates() {
		// FIXME
		// if (this.source.getSource().isSigned() && this.certificates == null) {
		// this.source.getSource().setupEntryCertificates();
		// }
		return this.certificates;
	}

	@Override
	public CodeSigner[] getCodeSigners() {
		// FIXME
		// if (this.source.getSource().isSigned() && this.codeSigners == null) {
		// this.source.getSource().setupEntryCertificates();
		// }
		return this.codeSigners;
	}

	void setupCertificates(java.util.jar.JarEntry entry) {
		this.certificates = entry.getCertificates();
		this.codeSigners = entry.getCodeSigners();
	}

	public void setLocalHeaderOffset(long localHeaderOffset) {
		this.localHeaderOffset = localHeaderOffset;
	}

	public long getLocalHeaderOffset() {
		return this.localHeaderOffset;
	}

	// FIXME
	// boolean isSigned() {
	// return this.signed;
	// }
	//
	// void setupEntryCertificates() {
	// // Fallback to JarInputStream to obtain certificates, not fast but hopefully not
	// // happening that often.
	// try {
	// JarInputStream inputStream = new JarInputStream(
	// getData().getInputStream(ResourceAccess.ONCE));
	// try {
	// java.util.jar.JarEntry entry = inputStream.getNextJarEntry();
	// while (entry != null) {
	// inputStream.closeEntry();
	// JarEntry jarEntry = getJarEntry(entry.getName());
	// if (jarEntry != null) {
	// jarEntry.setupCertificates(entry);
	// }
	// entry = inputStream.getNextJarEntry();
	// }
	// }
	// finally {
	// inputStream.close();
	// }
	// }
	// catch (IOException ex) {
	// throw new IllegalStateException(ex);
	// }
	// }

}
