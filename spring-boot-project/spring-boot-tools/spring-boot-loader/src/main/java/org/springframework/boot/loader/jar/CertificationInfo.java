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
import java.io.UncheckedIOException;
import java.util.jar.JarInputStream;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * @author Phillip Webb
 */
class CertificationInfo {

	private static final CertificationInfo NONE = new CertificationInfo();

	CertificationInfo get(ZipContent content) {
		if (!content.hasJarSignatureFile()) {
			return NONE;
		}
		try {
			load(content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		// @formatter:off

//		certifications = new JarEntryCertification[this.size];
//		// We fall back to use JarInputStream to obtain the certs. This isn't that
//		// fast, but hopefully doesn't happen too often.
//		try (JarInputStream certifiedJarStream = new JarInputStream(this.jarFile.getData().getInputStream())) {
//			java.util.jar.JarEntry certifiedEntry;
//			while ((certifiedEntry = certifiedJarStream.getNextJarEntry()) != null) {
//				// Entry must be closed to trigger a read and set entry certificates
//				certifiedJarStream.closeEntry();
//				int index = getEntryIndex(certifiedEntry.getName());
//				if (index != -1) {
//					certifications[index] = JarEntryCertification.from(certifiedEntry);
//				}
//			}
//		}
//		this.certifications = certifications;


// @formatter:on

		return null;
	}

	private void load(ZipContent content) throws IOException {
		try (JarInputStream in = openJarInputStream(content)) {
			java.util.jar.JarEntry entry;
			while ((entry = in.getNextJarEntry()) != null) {
				in.closeEntry(); // Close to trigger a read and set certs/signers
				int index = getIndex(entry.getName());
				if (index != -1) {
					entry.getCertificates();
					entry.getCodeSigners();
				}
			}
		}
	}

	/**
	 * @param name
	 * @return
	 */
	private int getIndex(String name) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param content
	 * @return
	 */
	private JarInputStream openJarInputStream(ZipContent content) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
