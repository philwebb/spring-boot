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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

/**
 * Writes JAR content, ensuring valid directory entries are always created and duplicate
 * items are ignored.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
public class JarWriter extends AbstractJarWriter implements AutoCloseable {

	private final JarArchiveOutputStream jarOutput;

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public JarWriter(File file) throws FileNotFoundException, IOException {
		this(file, null);
	}

	/**
	 * Create a new {@link JarWriter} instance.
	 * @param file the file to write
	 * @param launchScript an optional launch script to prepend to the front of the jar
	 * @throws IOException if the file cannot be opened
	 * @throws FileNotFoundException if the file cannot be found
	 */
	public JarWriter(File file, LaunchScript launchScript) throws FileNotFoundException, IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		if (launchScript != null) {
			fileOutputStream.write(launchScript.toByteArray());
			setExecutableFilePermission(file);
		}
		this.jarOutput = new JarArchiveOutputStream(fileOutputStream);
		this.jarOutput.setEncoding("UTF-8");
	}

	private void setExecutableFilePermission(File file) {
		try {
			Path path = file.toPath();
			Set<PosixFilePermission> permissions = new HashSet<>(Files.getPosixFilePermissions(path));
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(path, permissions);
		}
		catch (Throwable ex) {
			// Ignore and continue creating the jar
		}
	}

	@Override
	protected void writeEntry(JarArchiveEntry entry, EntryWriter entryWriter) throws IOException {
		this.jarOutput.putArchiveEntry(entry);
		if (entryWriter != null) {
			entryWriter.write(this.jarOutput);
		}
		this.jarOutput.closeArchiveEntry();
	}

	/**
	 * Close the writer.
	 * @throws IOException if the file cannot be closed
	 */
	@Override
	public void close() throws IOException {
		this.jarOutput.close();
	}

}
