/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.boot.build.antora;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

/**
 * A contribution of source to Antora.
 *
 * @author Andy Wilkinson
 */
class SourceContribution extends Contribution {

	private static final String CONFIGURATION_NAME = "antoraSource";

	private final Project project;

	SourceContribution(Project project) {
		this.project = project;
	}

	void produce() {
		Configuration antoraSource = this.project.getConfigurations().create(CONFIGURATION_NAME);
		TaskProvider<Zip> antoraSourceZip = this.project.getTasks().register("antoraSourceZip", Zip.class, (zip) -> {
			zip.getDestinationDirectory().set(this.project.getLayout().getBuildDirectory().dir("antora-source"));
			zip.from("src/docs/antora");
			zip.setDescription("Creates a zip archive of the Antora source in src/docs/antora.");
		});
		this.project.getArtifacts().add(antoraSource.getName(), antoraSourceZip);
	}

	void consumeFrom(String name, String path) {
		Configuration antoraSourceConfiguration = this.project.getConfigurations()
			.create("%sAntoraSource".formatted(toCamelCase(name)));
		this.project.getDependencies()
			.add(antoraSourceConfiguration.getName(), this.project.provider(() -> this.project.getDependencies()
				.project(Map.of("path", path, "configuration", CONFIGURATION_NAME))));
		Provider<Directory> sourceOutputDirectory = this.project.getLayout()
			.getBuildDirectory()
			.dir("generated/docs/antora-dependencies-source/" + name);
		this.project.getTasks()
			.named("generateAntoraPlaybook", GenerateAntoraPlaybook.class,
					(generatePlaybook) -> generatePlaybook.getContentSource().addStartPath(sourceOutputDirectory));
		TaskProvider<SyncAntoraSource> sync = this.project.getTasks()
			.register("sync%sAntoraSource".formatted(toPascalCase(name)), SyncAntoraSource.class, (task) -> {
				task.setSource(antoraSourceConfiguration);
				task.getOutputDirectory().set(sourceOutputDirectory);
				task.setDescription("Syncs the %s Antora source from %s.".formatted(name, path));
			});
		this.project.getTasks()
			.named("antora",
					(task) -> task.getInputs()
						.dir(sync.map(SyncAntoraSource::getOutputDirectory))
						.withPathSensitivity(PathSensitivity.RELATIVE)
						.withPropertyName(antoraSourceConfiguration.getName()));
	}

	static abstract class SyncAntoraSource extends DefaultTask {

		private final FileSystemOperations fileSystemOperations;

		private final ArchiveOperations archiveOperations;

		private FileCollection source;

		@Inject
		public SyncAntoraSource(FileSystemOperations fileSystemOperations, ArchiveOperations archiveOperations) {
			this.fileSystemOperations = fileSystemOperations;
			this.archiveOperations = archiveOperations;
		}

		@OutputDirectory
		public abstract DirectoryProperty getOutputDirectory();

		@InputFiles
		public FileCollection getSource() {
			return this.source;
		}

		public void setSource(FileCollection source) {
			this.source = source;
		}

		@TaskAction
		void syncAntoraSource() {
			this.fileSystemOperations.sync((sync) -> {
				sync.into(getOutputDirectory());
				this.source.getFiles().forEach((file) -> sync.from(this.archiveOperations.zipTree(file)));
			});
		}

	}

}
