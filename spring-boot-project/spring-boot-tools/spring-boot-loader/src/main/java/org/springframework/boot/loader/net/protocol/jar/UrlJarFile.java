/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.springframework.boot.loader.net.protocol.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@link JarFile} subclass returned from a {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 */
class UrlJarFile extends JarFile {

	private final UrlJarManifest manifest;

	private final Consumer<JarFile> closeAction;

	private UrlJarFile(File file, Runtime.Version version, Consumer<JarFile> closeAction) throws IOException {
		super(file, true, ZipFile.OPEN_READ | ZipFile.OPEN_DELETE, version);
		this.manifest = new UrlJarManifest(super::getManifest);
		this.closeAction = closeAction;
	}

	@Override
	public ZipEntry getEntry(String name) {
		return UrlJarEntry.of(this.getEntry(name), this.manifest);
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.manifest.get();
	}

	@Override
	public void close() throws IOException {
		this.closeAction.accept(this);
		super.close();
	}

	// FIXME here down

	static JarFile getJarFile(URL url) throws IOException {
		return getJarFile(url, null);
	}

	static JarFile getJarFile(URL url, Consumer<JarFile> closeController) throws IOException {
		if (isFileURL(url)) {
			Runtime.Version version = getVersion(url);
			File file = new File(ParseUtil.decode(url.getFile()));
			return new UrlJarFile(file, version, closeController);
		}
		else {
			return retrieve(url, closeController);
		}
	}

	/**
	 * Given a URL, retrieves a JAR file, caches it to disk, and creates a cached JAR file
	 * object.
	 */
	private static JarFile retrieve(final URL url, final Consumer<JarFile> closeAction) throws IOException {
		JarFile result = null;
		Runtime.Version version = getVersion(url);
		try (final InputStream in = url.openConnection().getInputStream()) {
			result = extracted(closeAction, version, in);
		}
		return result;
	}

	private static JarFile extracted(final Consumer<JarFile> closeController, Runtime.Version version, InputStream in)
			throws IOException {
		Path tmpFile = Files.createTempFile("jar_cache", null);
		try {
			Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING);
			JarFile jarFile = new UrlJarFile(tmpFile.toFile(), version, closeController);
			tmpFile.toFile().deleteOnExit();
			return jarFile;
		}
		catch (Throwable thr) {
			try {
				Files.delete(tmpFile);
			}
			catch (IOException ioe) {
				thr.addSuppressed(ioe);
			}
			throw thr;
		}
	}

	static boolean isFileURL(URL url) {
		if (url.getProtocol().equalsIgnoreCase("file")) {
			/*
			 * Consider this a 'file' only if it's a LOCAL file, because 'file:' URLs can
			 * be accessible through ftp.
			 */
			String host = url.getHost();
			if (host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost")) {
				return true;
			}
		}
		return false;
	}

	private static Runtime.Version getVersion(URL url) {
		return "runtime".equals(url.getRef()) ? JarFile.runtimeVersion() : JarFile.baseVersion();
	}

}
