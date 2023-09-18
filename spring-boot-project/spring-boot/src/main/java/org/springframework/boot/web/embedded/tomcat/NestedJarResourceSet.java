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

package org.springframework.boot.web.embedded.tomcat;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.AbstractSingleArchiveResourceSet;
import org.apache.catalina.webresources.JarResource;

/**
 * A {@link WebResourceSet} based on a nested JAR file.
 *
 * @author Phillip Webb
 */
class NestedJarResourceSet extends AbstractSingleArchiveResourceSet {

	private final URL url;

	private JarFile archive = null;

	private long archiveUseCount = 0;

	NestedJarResourceSet(URL url, WebResourceRoot root, String webAppMount, String internalPath)
			throws IllegalArgumentException {
		this.url = url;
		setRoot(root);
		setWebAppMount(webAppMount);
		setInternalPath(internalPath);
		setStaticOnly(true);
		if (getRoot().getState().isAvailable()) {
			try {
				start();
			}
			catch (LifecycleException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	protected WebResource createArchiveResource(JarEntry jarEntry, String webAppPath, Manifest manifest) {
		return new JarResource(this, webAppPath, getBaseUrlString(), jarEntry);
	}

	@Override
	protected void initInternal() throws LifecycleException {
		try {
			JarURLConnection connection = (JarURLConnection) this.url.openConnection();
			try {
				setManifest(connection.getManifest());
				setBaseUrl(connection.getJarFileURL());
			}
			finally {
				if (!connection.getUseCaches()) {
					connection.getJarFile().close();
				}
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected JarFile openJarFile() throws IOException {
		synchronized (this.archiveLock) {
			if (this.archive == null) {
				JarURLConnection connection = (JarURLConnection) this.url.openConnection();
				this.archive = connection.getJarFile();
			}
			this.archiveUseCount++;
			return this.archive;
		}
	}

	@Override
	protected void closeJarFile() {
		synchronized (this.archiveLock) {
			this.archiveUseCount--;
		}
	}

	@Override
	public void gc() {
		synchronized (this.archiveLock) {
			if (this.archive != null && this.archiveUseCount == 0) {
				try {
					this.archive.close();
				}
				catch (IOException ex) {
					// Ignore
				}
				this.archive = null;
				this.archiveEntries = null;
			}
		}
	}

}
