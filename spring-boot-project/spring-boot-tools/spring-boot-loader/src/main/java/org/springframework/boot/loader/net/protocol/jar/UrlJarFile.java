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

	UrlJarFile(File file, Runtime.Version version, Consumer<JarFile> closeAction) throws IOException {
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

}
