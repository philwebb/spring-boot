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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.compress.archivers.jar.JarArchiveEntry;

import org.springframework.boot.loader.tools.AbstractJarWriter.EntryTransformer;
import org.springframework.boot.loader.tools.AbstractJarWriter.UnpackHandler;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for packagers.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public abstract class AbstractPackager {

	private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";

	private static final String START_CLASS_ATTRIBUTE = "Start-Class";

	private static final String BOOT_LIB_ATTRIBUTE = "Spring-Boot-Lib";

	private static final String BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes";

	private static final String BOOT_VERSION_ATTRIBUTE = "Spring-Boot-Version";

	private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

	private static final long FIND_WARNING_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final File source;

	private Layout layout;

	private LayoutFactory layoutFactory;

	private List<MainClassTimeoutWarningListener> mainClassTimeoutListeners = new ArrayList<>();

	private String mainClass;

	AbstractPackager(File source, LayoutFactory layoutFactory) {
		if (source == null) {
			throw new IllegalArgumentException("Source file must be provided");
		}
		if (!source.exists() || !source.isFile()) {
			throw new IllegalArgumentException(
					"Source must refer to an existing file, got " + source.getAbsolutePath());
		}
		this.source = source.getAbsoluteFile();
		this.layoutFactory = layoutFactory;
	}

	/**
	 * Add a listener that will be triggered to display a warning if searching for the
	 * main class takes too long.
	 * @param listener the listener to add
	 */
	public void addMainClassTimeoutWarningListener(MainClassTimeoutWarningListener listener) {
		this.mainClassTimeoutListeners.add(listener);
	}

	/**
	 * Sets the main class that should be run. If not specified the value from the
	 * MANIFEST will be used, or if no manifest entry is found the archive will be
	 * searched for a suitable class.
	 * @param mainClass the main class name
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Sets the layout to use for the jar. Defaults to {@link Layouts#forFile(File)}.
	 * @param layout the layout
	 */
	public void setLayout(Layout layout) {
		if (layout == null) {
			throw new IllegalArgumentException("Layout must not be null");
		}
		this.layout = layout;
	}

	/**
	 * Sets the layout factory for the jar. The factory can be used when no specific
	 * layout is specified.
	 * @param layoutFactory the layout factory to set
	 */
	public void setLayoutFactory(LayoutFactory layoutFactory) {
		this.layoutFactory = layoutFactory;
	}

	void write(JarFile sourceJar, WritableLibraries writeableLibraries, AbstractJarWriter writer) throws IOException {
		writer.writeManifest(buildManifest(sourceJar));
		writeLoaderClasses(writer);
		if (getLayout() instanceof RepackagingLayout) {
			writer.writeEntries(sourceJar,
					new RenamingEntryTransformer(((RepackagingLayout) this.layout).getRepackagedClassesLocation()),
					writeableLibraries);
		}
		else {
			writer.writeEntries(sourceJar, writeableLibraries);
		}
		writeableLibraries.write(writer);
	}

	private void writeLoaderClasses(AbstractJarWriter writer) throws IOException {
		Layout layout = getLayout();
		if (layout instanceof CustomLoaderLayout) {
			((CustomLoaderLayout) this.layout).writeLoadedClasses(writer);
		}
		else if (layout.isExecutable()) {
			writer.writeLoaderClasses();
		}
	}

	protected final boolean isAlreadyRepackaged() throws IOException {
		return isAlreadyRepackaged(getSource());
	}

	protected boolean isAlreadyRepackaged(File file) throws IOException {
		try (JarFile jarFile = new JarFile(file)) {
			Manifest manifest = jarFile.getManifest();
			return (manifest != null && manifest.getMainAttributes().getValue(BOOT_VERSION_ATTRIBUTE) != null);
		}
	}

	private boolean isZip(File file) {
		try {
			try (FileInputStream fileInputStream = new FileInputStream(file)) {
				return isZip(fileInputStream);
			}
		}
		catch (IOException ex) {
			return false;
		}
	}

	private boolean isZip(InputStream inputStream) throws IOException {
		for (byte magicByte : ZIP_FILE_HEADER) {
			if (inputStream.read() != magicByte) {
				return false;
			}
		}
		return true;
	}

	private Manifest buildManifest(JarFile source) throws IOException {
		Manifest manifest = source.getManifest();
		if (manifest == null) {
			manifest = new Manifest();
			manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		}
		manifest = new Manifest(manifest);
		String startClass = this.mainClass;
		if (startClass == null) {
			startClass = manifest.getMainAttributes().getValue(MAIN_CLASS_ATTRIBUTE);
		}
		if (startClass == null) {
			startClass = findMainMethodWithTimeoutWarning(source);
		}
		Layout layout = getLayout();
		String launcherClassName = layout.getLauncherClassName();
		if (launcherClassName != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, launcherClassName);
			if (startClass == null) {
				throw new IllegalStateException("Unable to find main class");
			}
			manifest.getMainAttributes().putValue(START_CLASS_ATTRIBUTE, startClass);
		}
		else if (startClass != null) {
			manifest.getMainAttributes().putValue(MAIN_CLASS_ATTRIBUTE, startClass);
		}
		String bootVersion = getClass().getPackage().getImplementationVersion();
		manifest.getMainAttributes().putValue(BOOT_VERSION_ATTRIBUTE, bootVersion);
		manifest.getMainAttributes().putValue(BOOT_CLASSES_ATTRIBUTE, (layout instanceof RepackagingLayout)
				? ((RepackagingLayout) layout).getRepackagedClassesLocation() : layout.getClassesLocation());
		String lib = layout.getLibraryDestination("", LibraryScope.COMPILE);
		if (StringUtils.hasLength(lib)) {
			manifest.getMainAttributes().putValue(BOOT_LIB_ATTRIBUTE, lib);
		}
		return manifest;
	}

	private String findMainMethodWithTimeoutWarning(JarFile source) throws IOException {
		long startTime = System.currentTimeMillis();
		String mainMethod = findMainMethod(source);
		long duration = System.currentTimeMillis() - startTime;
		if (duration > FIND_WARNING_TIMEOUT) {
			for (MainClassTimeoutWarningListener listener : this.mainClassTimeoutListeners) {
				listener.handleTimeoutWarning(duration, mainMethod);
			}
		}
		return mainMethod;
	}

	protected String findMainMethod(JarFile source) throws IOException {
		return MainClassFinder.findSingleMainClass(source, getLayout().getClassesLocation(),
				SPRING_BOOT_APPLICATION_CLASS_NAME);
	}

	protected final File getSource() {
		return this.source;
	}

	protected final Layout getLayout() {
		if (this.layout == null) {
			this.layout = getLayoutFactory().getLayout(this.source);
		}
		return this.layout;
	}

	private LayoutFactory getLayoutFactory() {
		if (this.layoutFactory != null) {
			return this.layoutFactory;
		}
		List<LayoutFactory> factories = SpringFactoriesLoader.loadFactories(LayoutFactory.class, null);
		if (factories.isEmpty()) {
			return new DefaultLayoutFactory();
		}
		Assert.state(factories.size() == 1, "No unique LayoutFactory found");
		return factories.get(0);
	}

	/**
	 * Return the {@link File} to use to backup the original source.
	 * @return the file to use to backup the original source
	 */
	public final File getBackupFile() {
		return new File(getSource().getParentFile(), getSource().getName() + ".original");
	}

	/**
	 * Callback interface used to present a warning when finding the main class takes too
	 * long.
	 */
	@FunctionalInterface
	public interface MainClassTimeoutWarningListener {

		/**
		 * Handle a timeout warning.
		 * @param duration the amount of time it took to find the main method
		 * @param mainMethod the main method that was actually found
		 */
		void handleTimeoutWarning(long duration, String mainMethod);

	}

	/**
	 * An {@code EntryTransformer} that renames entries by applying a prefix.
	 */
	private static final class RenamingEntryTransformer implements EntryTransformer {

		private final String namePrefix;

		private RenamingEntryTransformer(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public JarArchiveEntry transform(JarArchiveEntry entry) {
			if (entry.getName().equals("META-INF/INDEX.LIST")) {
				return null;
			}
			if ((entry.getName().startsWith("META-INF/") && !entry.getName().equals("META-INF/aop.xml")
					&& !entry.getName().endsWith(".kotlin_module")) || entry.getName().startsWith("BOOT-INF/")
					|| entry.getName().equals("module-info.class")) {
				return entry;
			}
			JarArchiveEntry renamedEntry = new JarArchiveEntry(this.namePrefix + entry.getName());
			renamedEntry.setTime(entry.getTime());
			renamedEntry.setSize(entry.getSize());
			renamedEntry.setMethod(entry.getMethod());
			if (entry.getComment() != null) {
				renamedEntry.setComment(entry.getComment());
			}
			renamedEntry.setCompressedSize(entry.getCompressedSize());
			renamedEntry.setCrc(entry.getCrc());
			if (entry.getCreationTime() != null) {
				renamedEntry.setCreationTime(entry.getCreationTime());
			}
			if (entry.getExtra() != null) {
				renamedEntry.setExtra(entry.getExtra());
			}
			if (entry.getLastAccessTime() != null) {
				renamedEntry.setLastAccessTime(entry.getLastAccessTime());
			}
			if (entry.getLastModifiedTime() != null) {
				renamedEntry.setLastModifiedTime(entry.getLastModifiedTime());
			}
			return renamedEntry;
		}

	}

	/**
	 * An {@link UnpackHandler} that determines that an entry needs to be unpacked if a
	 * library that requires unpacking has a matching entry name.
	 */
	final class WritableLibraries implements UnpackHandler {

		private final Map<String, Library> libraryEntryNames = new LinkedHashMap<>();

		WritableLibraries(Libraries libraries) throws IOException {
			libraries.doWithLibraries((library) -> {
				if (isZip(library.getFile())) {
					String libraryDestination = getLayout().getLibraryDestination(library.getName(),
							library.getScope());
					if (libraryDestination != null) {
						Library existing = this.libraryEntryNames.putIfAbsent(libraryDestination + library.getName(),
								library);
						if (existing != null) {
							throw new IllegalStateException("Duplicate library " + library.getName());
						}
					}
				}
			});
		}

		@Override
		public boolean requiresUnpack(String name) {
			Library library = this.libraryEntryNames.get(name);
			return library != null && library.isUnpackRequired();
		}

		@Override
		public String sha1Hash(String name) throws IOException {
			Library library = this.libraryEntryNames.get(name);
			if (library == null) {
				throw new IllegalArgumentException("No library found for entry name '" + name + "'");
			}
			return FileUtils.sha1Hash(library.getFile());
		}

		private void write(AbstractJarWriter writer) throws IOException {
			for (Entry<String, Library> entry : this.libraryEntryNames.entrySet()) {
				writer.writeNestedLibrary(entry.getKey().substring(0, entry.getKey().lastIndexOf('/') + 1),
						entry.getValue());
			}
		}

	}

}
