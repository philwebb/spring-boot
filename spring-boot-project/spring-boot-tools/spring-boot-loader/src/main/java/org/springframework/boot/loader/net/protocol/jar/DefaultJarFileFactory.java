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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runtime.Version;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * @author pwebb
 */
public class DefaultJarFileFactory implements JarFileFactory {

	@Override
	public JarFile createJarFile(URL jarFileUrl, Consumer<JarFile> closeAction) throws IOException {
		Runtime.Version version = getVersion(jarFileUrl);
		if (isLocalFileUrl(jarFileUrl)) {
			return createJarFileForLocalFile(jarFileUrl, version, closeAction);
		}
		if (isNestedJarEntryUrl(jarFileUrl)) {
			// FIXME createJarFileForNestedLocalFile
		}
		return createJarFileForRemoteFile(jarFileUrl, version, closeAction);
	}

	private Runtime.Version getVersion(URL url) {
		return "runtime".equals(url.getRef()) ? JarFile.runtimeVersion() : JarFile.baseVersion();
	}

	private boolean isLocalFileUrl(URL url) {
		return url.getProtocol().equalsIgnoreCase("file") && isLocal(url.getHost());
	}

	private boolean isLocal(String host) {
		return host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost");
	}

	private boolean isNestedJarEntryUrl(URL url) {
		return url.getProtocol().equalsIgnoreCase("nested");
	}

	private JarFile createJarFileForLocalFile(URL url, Runtime.Version version, Consumer<JarFile> closeAction)
			throws IOException {
		File file = new File(UrlDecoder.decode(url.getFile()));
		return new UrlJarFile(file, version, closeAction);
	}

	private JarFile createJarFileForRemoteFile(URL url, Version version, Consumer<JarFile> closeAction)
			throws IOException {
		try (InputStream in = url.openStream()) {
			return createJarFileForRemoteFile(in, version, closeAction);
		}
	}

	private JarFile createJarFileForRemoteFile(InputStream in, Version version, Consumer<JarFile> closeAction)
			throws IOException {
		Path local = Files.createTempFile("jar_cache", null);
		try {
			Files.copy(in, local, StandardCopyOption.REPLACE_EXISTING);
			JarFile jarFile = new UrlJarFile(local.toFile(), version, closeAction);
			local.toFile().deleteOnExit();
			return jarFile;
		}
		catch (Throwable ex) {
			deleteIfPossible(local, ex);
			throw ex;
		}
	}

	private void deleteIfPossible(Path local, Throwable cause) {
		try {
			Files.delete(local);
		}
		catch (IOException ex) {
			cause.addSuppressed(ex);
		}
	}

}
