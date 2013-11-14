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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;

/**
 * {@link java.net.JarURLConnection} used to support {@link JarFile#getUrl()}.
 * 
 * @author Phillip Webb
 */
class JarURLConnection extends java.net.JarURLConnection {

	private JarFile jarFile;

	private JarEntry jarEntry;

	private String jarEntryName;

	private String contentType;

	protected JarURLConnection(URL url, JarFile jarFile) throws MalformedURLException {
		super(new URL("jar:file:" + jarFile.getRootJarFile().getFile().getPath() + "!/"));
		this.jarFile = jarFile;

		String spec = url.getFile();
		int separator = spec.lastIndexOf("!/");
		if (separator == -1) {
			throw new MalformedURLException("no !/ found in url spec:" + spec);
		}
		if (separator + 2 != spec.length()) {
			this.jarEntryName = spec.substring(separator + 2);
		}
	}

	@Override
	public void connect() throws IOException {
		if (this.jarEntryName != null) {
			this.jarEntry = this.jarFile.getJarEntry(this.jarEntryName);
			if (this.jarEntry == null) {
				throw new FileNotFoundException("JAR entry " + this.jarEntryName
						+ " not found in " + this.jarFile.getName());
			}
		}
		this.connected = true;
	}

	@Override
	public JarFile getJarFile() throws IOException {
		connect();
		return this.jarFile;
	}

	@Override
	public JarEntry getJarEntry() throws IOException {
		connect();
		return this.jarEntry;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		if (this.jarEntryName == null) {
			throw new IOException("no entry name specified");
		}
		return this.jarFile.getInputStream(this.jarEntry);
	}

	@Override
	public int getContentLength() {
		try {
			connect();
			return (int) (this.jarEntry == null ? this.jarFile.size() : this.jarEntry
					.getSize());
		}
		catch (IOException ex) {
			return -1;
		}
	}

	@Override
	public Object getContent() throws IOException {
		connect();
		return (this.jarEntry == null ? this.jarFile : super.getContent());
	}

	@Override
	public String getContentType() {
		if (this.contentType == null) {
			// Guess the content type, don't bother with steams as mark is not
			// supported
			this.contentType = (this.jarEntryName == null ? "x-java/jar" : null);
			this.contentType = (this.contentType == null ? guessContentTypeFromName(this.jarEntryName)
					: this.contentType);
			this.contentType = (this.contentType == null ? "content/unknown"
					: this.contentType);
		}
		return this.contentType;
	}
}
