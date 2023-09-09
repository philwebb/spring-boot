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

package org.springframework.boot.loader.net.protocol.jar;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * {@link java.net.JarURLConnection} alternative to
 * {@code sun.net.www.protocol.jar.JarURLConnection} with optimized support for nested
 * jars.
 *
 * @author Phillip Webb
 */
class JarUrlConnection extends java.net.JarURLConnection {

	private static final ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();

	private static final UrlJarFiles jarFiles = new UrlJarFiles();

	private static final IOException NO_ENTRY_NAME_SPECIFIED_EXCEPTION = new IOException("no entry name specified");

	private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException(
			"Jar file or entry not found");

	private final String entryName;

	private JarFile jarFile;

	private URLConnection jarFileConnection;

	private JarEntry jarEntry;

	private String contentType;

	JarUrlConnection(URL url) throws IOException {
		super(url);
		this.entryName = getEntryName();
		this.jarFileConnection = getJarFileURL().openConnection();
		this.jarFileConnection.setUseCaches(this.useCaches);
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
	public Permission getPermission() throws IOException {
		return this.jarFileConnection.getPermission();
	}

	@Override
	public void connect() throws IOException {
		if (this.connected) {
			return;
		}
		boolean useCaches = getUseCaches();
		URL jarFileURL = getJarFileURL();
		this.jarFile = jarFiles.getOrCreate(useCaches, jarFileURL);
		this.jarEntry = getJarEntry(jarFileURL);
		boolean addedToCache = jarFiles.cacheIfAbsent(useCaches, jarFileURL, this.jarFile);
		if (addedToCache) {
			this.jarFileConnection = jarFiles.reconnect(this.jarFile, this.jarFileConnection);
		}
		this.connected = true;
	}

	private JarEntry getJarEntry(URL jarFileUrl) throws IOException {
		if (this.entryName == null) {
			return null;
		}
		JarEntry jarEntry = this.jarFile.getJarEntry(this.entryName);
		if (jarEntry == null) {
			jarFiles.closeIfNotCached(jarFileUrl, this.jarFile);
			throwFileNotFound();
		}
		return jarEntry;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		connect();
		if (this.entryName == null) {
			throw NO_ENTRY_NAME_SPECIFIED_EXCEPTION;
		}
		if (this.jarEntry == null) {
			throwFileNotFound();
		}
		return new ConnectionInputStream(this.jarFile.getInputStream(this.jarEntry));
	}

	private void throwFileNotFound() throws FileNotFoundException {
		if (Boolean.TRUE.equals(useFastExceptions.get())) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
		throw new FileNotFoundException("JAR entry " + this.entryName + " not found in " + this.jarFile.getName());
	}

	@Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		return (contentLength <= Integer.MAX_VALUE) ? (int) contentLength : -1;
	}

	@Override
	public long getContentLengthLong() {
		try {
			connect();
			return (this.jarEntry != null) ? this.jarEntry.getSize() : this.jarFileConnection.getContentLengthLong();
		}
		catch (IOException ex) {
			return -1;
		}
	}

	@Override
	public Object getContent() throws IOException {
		connect();
		return (this.entryName != null) ? super.getContent() : this.jarFile;
	}

	@Override
	public String getContentType() {
		if (this.contentType == null) {
			this.contentType = deduceContentType();
		}
		return this.contentType;
	}

	private String deduceContentType() {
		String type = this.entryName != null ? null : "x-java/jar";
		type = (type != null) ? type : deduceContentTypeFromStream();
		type = (type != null) ? type : deduceContentTypeFromEntryName();
		return (type != null) ? type : "content/unknown";
	}

	private String deduceContentTypeFromStream() {
		try {
			connect();
			try (InputStream in = this.jarFile.getInputStream(this.jarEntry)) {
				return guessContentTypeFromStream(new BufferedInputStream(in));
			}
		}
		catch (IOException ex) {
			return null;
		}
	}

	private String deduceContentTypeFromEntryName() {
		return guessContentTypeFromName(this.entryName);
	}

	@Override
	public String getHeaderField(String name) {
		return this.jarFileConnection.getHeaderField(name);
	}

	@Override
	public String getRequestProperty(String key) {
		return this.jarFileConnection.getRequestProperty(key);
	}

	@Override
	public void setRequestProperty(String key, String value) {
		this.jarFileConnection.setRequestProperty(key, value);
	}

	@Override
	public void addRequestProperty(String key, String value) {
		this.jarFileConnection.addRequestProperty(key, value);
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		return this.jarFileConnection.getRequestProperties();
	}

	@Override
	public boolean getAllowUserInteraction() {
		return this.jarFileConnection.getAllowUserInteraction();
	}

	@Override
	public void setAllowUserInteraction(boolean allowuserinteraction) {
		this.jarFileConnection.setAllowUserInteraction(allowuserinteraction);
	}

	@Override
	public boolean getUseCaches() {
		return this.jarFileConnection.getUseCaches();
	}

	@Override
	public void setUseCaches(boolean usecaches) {
		this.jarFileConnection.setUseCaches(usecaches);
	}

	@Override
	public void setIfModifiedSince(long ifmodifiedsince) {
		this.jarFileConnection.setIfModifiedSince(ifmodifiedsince);
	}

	@Override
	public boolean getDefaultUseCaches() {
		return this.jarFileConnection.getDefaultUseCaches();
	}

	@Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
		this.jarFileConnection.setDefaultUseCaches(defaultusecaches);
	}

	static void useFastExceptions(boolean useFastExceptions) {
		JarUrlConnection.useFastExceptions.set(useFastExceptions);
	}

	/**
	 * Connection {@link InputStream}
	 */
	class ConnectionInputStream extends FilterInputStream {

		ConnectionInputStream(InputStream in) {
			super(in);
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			}
			finally {
				if (!getUseCaches()) {
					JarUrlConnection.this.jarFile.close();
				}
			}
		}

	}

}
