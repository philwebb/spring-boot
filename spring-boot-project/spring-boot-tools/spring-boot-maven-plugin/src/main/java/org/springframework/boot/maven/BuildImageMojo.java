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

package org.springframework.boot.maven;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;

import org.springframework.boot.loader.tools.EntryWriter;
import org.springframework.boot.loader.tools.ExportPackager;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.RepeatableEntryWriter;
import org.springframework.boot.pack.build.AbstractBuildLog;
import org.springframework.boot.pack.build.BuildLog;
import org.springframework.boot.pack.build.BuildRequest;
import org.springframework.boot.pack.build.Builder;
import org.springframework.boot.pack.docker.TotalProgressEvent;
import org.springframework.boot.pack.docker.type.ImageReference;
import org.springframework.boot.pack.io.Owner;
import org.springframework.boot.pack.io.TarArchive;

/**
 * Package an application into a OCI image using a
 * <a href="https://buildpacks.io">buildpack</a>.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
@Mojo(name = "build-image", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BuildImageMojo extends AbstractDependencyFilterMojo {

	/**
	 * The Maven project.
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * Maven project helper utils.
	 * @since 2.3.0
	 */
	@Component
	private MavenProjectHelper projectHelper;

	/**
	 * Skip the execution.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.build-image.skip", defaultValue = "false")
	private boolean skip;

	/**
	 * The name of the main class. If not specified the first compiled class found that
	 * contains a 'main' method will be used.
	 * @since 2.3.0
	 */
	@Parameter
	private String mainClass;

	/**
	 * Exclude Spring Boot devtools from the repackaged archive.
	 * @since 2.3.0
	 */
	@Parameter(property = "spring-boot.repackage.excludeDevtools", defaultValue = "true")
	private boolean excludeDevtools = true;

	/**
	 * Include system scoped dependencies.
	 * @since 2.3.0
	 */
	@Parameter(defaultValue = "false")
	public boolean includeSystemScope;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.project.getPackaging().equals("pom")) {
			getLog().debug("build-image goal could not be applied to pom project.");
			return;
		}
		if (this.skip) {
			getLog().debug("skipping build-image as per configuration.");
			return;
		}
		buildImage();
	}

	private void buildImage() throws MojoExecutionException {
		Set<Artifact> artifacts = filterDependencies(this.project.getArtifacts(), getFilters(getAdditionalFilters()));
		Libraries libraries = new ArtifactsLibraries(artifacts, Collections.emptySet(), getLog());
		try {
			Builder builder = new Builder(new MojoBuildLog(this::getLog));
			BuildRequest request = getBuildRequest(libraries);
			builder.build(request);
		}
		catch (IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	private BuildRequest getBuildRequest(Libraries libraries) {
		ImageReference name = ImageReference.forJarFile(this.project.getArtifact().getFile());
		return BuildRequest.of(name, (owner) -> getApplicationContent(owner, libraries));
	}

	private TarArchive getApplicationContent(Owner owner, Libraries libraries) {
		File source = this.project.getArtifact().getFile();
		ExportPackager packager = new ExportPackager(source);
		packager.addMainClassTimeoutWarningListener(new LoggingMainClassTimeoutWarningListener(this::getLog));
		packager.setMainClass(this.mainClass);
		return new PackagedTarArchive(owner, libraries, packager);
	}

	private ArtifactsFilter[] getAdditionalFilters() {
		List<ArtifactsFilter> filters = new ArrayList<>();
		if (this.excludeDevtools) {
			Exclude exclude = new Exclude();
			exclude.setGroupId("org.springframework.boot");
			exclude.setArtifactId("spring-boot-devtools");
			ExcludeFilter filter = new ExcludeFilter(exclude);
			filters.add(filter);
		}
		if (!this.includeSystemScope) {
			filters.add(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		}
		return filters.toArray(new ArtifactsFilter[0]);
	}

	/**
	 * {@link BuildLog} backed by Mojo logging.
	 */
	private static class MojoBuildLog extends AbstractBuildLog {

		private static final long THRESHOLD = Duration.ofSeconds(2).toMillis();

		private final Supplier<Log> log;

		MojoBuildLog(Supplier<Log> log) {
			this.log = log;
		}

		@Override
		protected void log(String message) {
			this.log.get().info(message);
		}

		@Override
		protected Consumer<TotalProgressEvent> getProgressConsumer(String message) {
			return new ProgressLog(message);
		}

		private class ProgressLog implements Consumer<TotalProgressEvent> {

			private final String message;

			private long last;

			ProgressLog(String message) {
				this.message = message;
				this.last = System.currentTimeMillis();
			}

			@Override
			public void accept(TotalProgressEvent progress) {
				log(progress.getPercent());
			}

			private void log(int percent) {
				if (percent == 100 || (System.currentTimeMillis() - this.last) > THRESHOLD) {
					MojoBuildLog.this.log.get().info(this.message + " " + percent + "%");
					this.last = System.currentTimeMillis();
				}
			}

		}

	}

	/**
	 * Adapter class to expose the packaged jar as a {@link TarArchive}.
	 */
	static class PackagedTarArchive implements TarArchive {

		static final long NORMALIZED_MOD_TIME = TarArchive.NORMALIZED_TIME.toEpochMilli();

		private final Owner owner;

		private final Libraries libraries;

		private final ExportPackager packager;

		PackagedTarArchive(Owner owner, Libraries libraries, ExportPackager packager) {
			this.owner = owner;
			this.libraries = libraries;
			this.packager = packager;
		}

		@Override
		public void writeTo(OutputStream outputStream) throws IOException {
			TarArchiveOutputStream tar = new TarArchiveOutputStream(outputStream);
			this.packager.export(this.libraries, (entry, entryWriter) -> write(entry, entryWriter, tar));
		}

		private void write(JarArchiveEntry jarEntry, EntryWriter entryWriter, TarArchiveOutputStream tar) {
			try {
				if (!jarEntry.isDirectory() && jarEntry.getSize() == ArchiveEntry.SIZE_UNKNOWN) {
					entryWriter = RepeatableEntryWriter.get(entryWriter);
					jarEntry.setSize(((RepeatableEntryWriter) entryWriter).size());
				}
				TarArchiveEntry tarEntry = convert(jarEntry);
				tar.putArchiveEntry(tarEntry);
				if (tarEntry.isFile()) {
					entryWriter.write(tar);
				}
				tar.closeArchiveEntry();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private TarArchiveEntry convert(JarArchiveEntry jarEntry) {
			byte linkFlag = (jarEntry.isDirectory()) ? TarConstants.LF_DIR : TarConstants.LF_NORMAL;
			TarArchiveEntry tarEntry = new TarArchiveEntry(jarEntry.getName(), linkFlag, true);
			tarEntry.setUserId(this.owner.getUid());
			tarEntry.setGroupId(this.owner.getGid());
			tarEntry.setModTime(NORMALIZED_MOD_TIME);
			if (!jarEntry.isDirectory()) {
				tarEntry.setSize(jarEntry.getSize());
			}
			return tarEntry;
		}

	}

}
