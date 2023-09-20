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
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Permission;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.boot.loader.jar.NestedJarFile;
import org.springframework.boot.loader.net.util.UrlDecoder;

/**
 * {@link java.net.JarURLConnection} alternative to
 * {@code sun.net.www.protocol.jar.JarURLConnection} with optimized support for nested
 * jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Rostyslav Dudka
 */
final class JarUrlConnection extends java.net.JarURLConnection {

	private static final ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();

	private static final UrlJarFiles jarFiles = new UrlJarFiles();

	private static final IOException NO_ENTRY_NAME_SPECIFIED_EXCEPTION = new IOException("no entry name specified");

	private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException(
			"Jar file or entry not found");

	private static final URL NOT_FOUND_URL;

	private static final JarUrlConnection NOT_FOUND_CONNECTION;
	static {
		try {
			NOT_FOUND_URL = new URL("jar:", null, 0, "nested:!/", new URLStreamHandler() {

				@Override
				protected URLConnection openConnection(URL u) {
					// Stub URLStreamHandler to prevent the wrong JAR Handler from being
					// Instantiated and cached.
					return null;
				}

			});
			NOT_FOUND_CONNECTION = new JarUrlConnection(() -> FILE_NOT_FOUND_EXCEPTION);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private final String entryName;

	private final Supplier<FileNotFoundException> notFound;

	private JarFile jarFile;

	private URLConnection jarFileConnection;

	private JarEntry jarEntry;

	private String contentType;

	private JarUrlConnection(URL url) throws IOException {
		super(url);
		this.entryName = getEntryName();
		this.notFound = null;
		this.jarFileConnection = getJarFileURL().openConnection();
		this.jarFileConnection.setUseCaches(this.useCaches);
	}

	private JarUrlConnection(Supplier<FileNotFoundException> notFound) throws IOException {
		super(NOT_FOUND_URL);
		this.entryName = null;
		this.notFound = notFound;
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
		if (this.notFound != null) {
			throw this.notFound.get();
		}
		boolean useCaches = getUseCaches();
		URL jarFileURL = getJarFileURL();
		if (this.entryName != null && Boolean.TRUE.equals(useFastExceptions.get())) {
			checkCachedForEntry(jarFileURL, this.entryName);
		}
		this.jarFile = jarFiles.getOrCreate(useCaches, jarFileURL);
		this.jarEntry = getJarEntry(jarFileURL);
		boolean addedToCache = jarFiles.cacheIfAbsent(useCaches, jarFileURL, this.jarFile);
		if (addedToCache) {
			this.jarFileConnection = jarFiles.reconnect(this.jarFile, this.jarFileConnection);
		}
		this.connected = true;
	}

	/**
	 * The {@link URLClassLoader} connect often to check if a resource exists, we can save
	 * some object allocations by using the cached copy if we have one.
	 * @param jarFileURL the jar file to check
	 * @param entryName the entry name to check
	 * @throws FileNotFoundException on a missing entry
	 */
	private void checkCachedForEntry(URL jarFileURL, String entryName) throws FileNotFoundException {
		JarFile cachedJarFile = jarFiles.getCached(jarFileURL);
		if (cachedJarFile != null && cachedJarFile.getJarEntry(entryName) == null) {
			throw FILE_NOT_FOUND_EXCEPTION;
		}
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
		return new ConnectionInputStream();
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
		String type = (this.entryName != null) ? null : "x-java/jar";
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
		return (this.jarFileConnection != null) ? this.jarFileConnection.getHeaderField(name) : null;
	}

	@Override
	public String getRequestProperty(String key) {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperty(key) : null;
	}

	@Override
	public void setRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setRequestProperty(key, value);
		}
	}

	@Override
	public void addRequestProperty(String key, String value) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.addRequestProperty(key, value);
		}
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getRequestProperties()
				: Collections.emptyMap();
	}

	@Override
	public boolean getAllowUserInteraction() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getAllowUserInteraction() : false;
	}

	@Override
	public void setAllowUserInteraction(boolean allowuserinteraction) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setAllowUserInteraction(allowuserinteraction);
		}
	}

	@Override
	public boolean getUseCaches() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getUseCaches() : true;
	}

	@Override
	public void setUseCaches(boolean usecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setUseCaches(usecaches);
		}
	}

	@Override
	public void setIfModifiedSince(long ifmodifiedsince) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setIfModifiedSince(ifmodifiedsince);
		}
	}

	@Override
	public boolean getDefaultUseCaches() {
		return (this.jarFileConnection != null) ? this.jarFileConnection.getDefaultUseCaches() : true;
	}

	@Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
		if (this.jarFileConnection != null) {
			this.jarFileConnection.setDefaultUseCaches(defaultusecaches);
		}
	}

	static void useFastExceptions(boolean useFastExceptions) {
		JarUrlConnection.useFastExceptions.set(useFastExceptions);
	}

	static URLConnection open(URL url) throws IOException {
		String spec = url.getFile();
		if (spec.startsWith("nested:")) {
			int separator = spec.indexOf("!/");
			boolean hasEntry = separator + 2 != spec.length();
			if (separator != -1 && hasEntry) {
				URL jarFileUrl = new URL(spec.substring(0, separator));
				if ("runtime".equals(url.getRef())) {
					jarFileUrl = new URL(jarFileUrl, "#runtime");
				}
				String entryName = UrlDecoder.decode(spec.substring(separator + 2));
				JarFile jarFile = jarFiles.getOrCreate(true, jarFileUrl);
				jarFiles.cacheIfAbsent(true, jarFileUrl, jarFile);
				if (!hasEntry(jarFile, entryName)) {
					return noFound(jarFile.getName(), entryName);
				}
			}
		}
		return new JarUrlConnection(url);
	}

	private static boolean hasEntry(JarFile jarFile, String name) {
		return (jarFile instanceof NestedJarFile nestedJarFile) ? nestedJarFile.hasEntry(name)
				: jarFile.getEntry(name) != null;
	}

	private static URLConnection noFound(String jarFileName, String entryName) throws IOException {
		if (Boolean.TRUE == useFastExceptions.get()) {
			return NOT_FOUND_CONNECTION;
		}
		return new JarUrlConnection(
				() -> new FileNotFoundException("JAR entry " + entryName + " not found in " + jarFileName));
	}

	/**
	 * Connection {@link InputStream}. This is not a {@link FilterInputStream} since
	 * {@link URLClassLoader} often creates streams that it doesn't call.
	 */
	class ConnectionInputStream extends InputStream {

		private volatile InputStream in;

		@Override
		public int read() throws IOException {
			return in().read();
		}

		@Override
		public int read(byte[] b) throws IOException {
			return in().read(b);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return in().read(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return in().skip(n);
		}

		@Override
		public int available() throws IOException {
			return in().available();
		}

		@Override
		public boolean markSupported() {
			try {
				return in().markSupported();
			}
			catch (IOException ex) {
				return false;
			}
		}

		@Override
		public synchronized void mark(int readlimit) {
			try {
				in().mark(readlimit);
			}
			catch (IOException ex) {
				// Ignore
			}
		}

		@Override
		public synchronized void reset() throws IOException {
			in().reset();
		}

		private InputStream in() throws IOException {
			InputStream in = this.in;
			if (in == null) {
				synchronized (this) {
					in = this.in;
					if (in == null) {
						in = JarUrlConnection.this.jarFile.getInputStream(JarUrlConnection.this.jarEntry);
						this.in = in;
					}
				}
			}
			return in;
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
