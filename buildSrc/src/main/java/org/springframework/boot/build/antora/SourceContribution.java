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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskInputFilePropertyBuilder;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

/**
 * A contribution of source to Antora.
 *
 * @author Andy Wilkinson
 */
class SourceContribution extends Contribution {

	private static final String CONFIGURATION_NAME = "antoraSource";

	SourceContribution(Project project, String name) {
		super(project, name);
	}

	void produce() {
		Configuration antoraSource = getProject().getConfigurations().create(CONFIGURATION_NAME);
		TaskProvider<Zip> antoraSourceZip = getProject().getTasks().register("antoraSourceZip", Zip.class, (zip) -> {
			zip.getDestinationDirectory().set(getProject().getLayout().getBuildDirectory().dir("antora-source"));
			zip.from("src/docs/antora");
			zip.setDescription("Creates a zip archive of the Antora source in src/docs/antora.");
		});
		getProject().getArtifacts().add(antoraSource.getName(), antoraSourceZip);
	}

	void consumeFrom(String path) {
		Configuration configuration = createConfiguration(getName());
		DependencyHandler dependencies = getProject().getDependencies();
		dependencies.add(configuration.getName(),
				getProject().provider(() -> projectDependency(path, CONFIGURATION_NAME)));
		Provider<Directory> outputDirectory = outputDirectory("source", getName());
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<SyncAntoraSource> syncSource = tasks.register(pascalCaseName("sync%sAntoraSource", getName()),
				SyncAntoraSource.class, (task) -> configureSyncSource(task, path, configuration, outputDirectory));
		tasks.named("antora", (task) -> addToTaskInputs(configuration, syncSource, task));
		tasks.named("generateAntoraPlaybook", GenerateAntoraPlaybook.class,
				(generatePlaybook) -> generatePlaybook.getContentSource().addStartPath(outputDirectory));
	}

	private TaskInputFilePropertyBuilder addToTaskInputs(Configuration configuration,
			TaskProvider<SyncAntoraSource> syncSource, Task task) {
		return task.getInputs()
			.dir(syncSource.map(SyncAntoraSource::getOutputDirectory))
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName(configuration.getName());
	}

	private void configureSyncSource(SyncAntoraSource task, String path, Configuration configuration,
			Provider<Directory> outputDirectory) {
		task.setDescription("Syncs the %s Antora source from %s.".formatted(getName(), path));
		task.setSource(configuration);
		task.getOutputDirectory().set(outputDirectory);
	}

	private Configuration createConfiguration(String name) {
		return getProject().getConfigurations().create(pascalCaseName("%sAntoraSource", name));
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
			this.fileSystemOperations.sync(this::syncAntoraSource);
		}

		private void syncAntoraSource(CopySpec sync) {
			sync.into(getOutputDirectory());
			this.source.getFiles().forEach((file) -> sync.from(this.archiveOperations.zipTree(file)));
		}

	}

}
