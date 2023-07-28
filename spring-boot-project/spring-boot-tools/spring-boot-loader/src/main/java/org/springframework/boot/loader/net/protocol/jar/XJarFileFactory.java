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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * @author pwebb
 */
public class XJarFileFactory {

	static JarFile createJarFile(URL jarFileUrl, Consumer<JarFile> closeAction) throws IOException {
		Runtime.Version version = getVersion(jarFileUrl);
		if (isFileURL(jarFileUrl)) {
			File file = new File(ParseUtil.decode(jarFileUrl.getFile()));
			return new UrlJarFile(file, version, closeAction);
		}
		if (isNestedUrl(jarFileUrl)) {

		}
		return null;
	}

	private static boolean isFileURL(URL url) {
		return url.getProtocol().equalsIgnoreCase("file") && isLocal(url.getHost());
	}

	private static boolean isLocal(String host) {
		return host == null || host.isEmpty() || host.equals("~") || host.equalsIgnoreCase("localhost");
	}

	private static boolean isNestedUrl(URL url) {
		return url.getProtocol().equalsIgnoreCase("nested");
	}

	// FIXME here down

	private static JarFile getJarFile(URL url, Consumer<JarFile> closeController) throws IOException {
		if (isFileURL(url)) {
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

	private static Runtime.Version getVersion(URL url) {
		return "runtime".equals(url.getRef()) ? JarFile.runtimeVersion() : JarFile.baseVersion();
	}

}
