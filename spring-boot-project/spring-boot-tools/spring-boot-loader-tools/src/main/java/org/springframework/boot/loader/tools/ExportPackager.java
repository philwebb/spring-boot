/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.jar.JarFile;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

/**
 * Utility class that can be used to export a fully packaged archive, for example to
 * create a Docker image.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class ExportPackager extends AbstractPackager {

	public ExportPackager(File source) {
		this(source, null);
	}

	public ExportPackager(File source, LayoutFactory layoutFactory) {
		super(source, layoutFactory);
	}

	public void export(Libraries libraries, BiConsumer<JarArchiveEntry, EntryWriter> action) throws IOException {
		File source = isAlreadyRepackaged() ? getBackupFile() : getSource();
		if (!source.exists() || !source.isFile()) {
			throw new IllegalStateException("Unable to read jar file " + source);
		}
		if (isAlreadyRepackaged(source)) {
			throw new IllegalStateException("Repackaged jar file " + source + " cannot be exported");
		}
		try (JarFile sourceJar = new JarFile(source)) {
			WritableLibraries writeableLibraries = new WritableLibraries(libraries);
			AbstractJarWriter writer = new AbstractJarWriter() {

				@Override
				protected void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException {
					action.accept(entry, entryWriter);
				}

			};
			write(sourceJar, writeableLibraries, writer);
		}

	}

}
