/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskInputFilePropertyBuilder;
import org.gradle.api.tasks.TaskProvider;

/**
 * A contribution of content to Antora that can be consumed by other projects.
 *
 * @author Andy Wilkinson
 */
class ConsumableContentContribution extends ContentContribution {

	protected ConsumableContentContribution(Project project, String type, String name) {
		super(project, name, type);
	}

	@Override
	void produceFrom(CopySpec copySpec) {
		TaskProvider<? extends Task> producer = super.configureProduction(copySpec);
		Configuration configuration = createConfiguration(getName());
		getProject().getArtifacts().add(configuration.getName(), producer);
	}

	void consumeFrom(String path) {
		Configuration configuration = createConfiguration(getName());
		DependencyHandler dependencies = getProject().getDependencies();
		dependencies.add(configuration.getName(), getProject().provider(() -> projectDependency(path, configuration)));
		Provider<Directory> outputDirectory = outputDirectory("content", getName());
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<?> copyAntoraContent = tasks.register(pascalCaseName("copy%sAntora%sContent", getName(), getType()),
				CopyAntoraContent.class, (task) -> configureCopyContent(task, path, configuration, outputDirectory));
		tasks.named("antora", (task) -> addToTaskInputs(task, copyAntoraContent, configuration.getName()));
		tasks.named("generateAntoraPlaybook", GenerateAntoraPlaybook.class,
				(task) -> addToZipContentsCollectorDependencies(task));
		getProject().getExtensions()
			.getByType(PublishingExtension.class)
			.getPublications()
			.withType(MavenPublication.class)
			.configureEach((mavenPublication) -> addPublishedMavenArtifact(mavenPublication, copyAntoraContent));
	}

	private void configureCopyContent(CopyAntoraContent task, String path, Configuration configuration,
			Provider<Directory> outputDirectory) {
		task.setDescription(
				"Syncs the %s Antora %s content from %s.".formatted(getName(), toDescription(getType()), path));
		task.setSource(configuration);
		task.getOutputFile().set(outputDirectory.map(this::getContentZipFile));
	}

	private TaskInputFilePropertyBuilder addToTaskInputs(Task task, TaskProvider<?> files, String propertyName) {
		return task.getInputs()
			.files(files)
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName(propertyName);
	}

	private void addToZipContentsCollectorDependencies(GenerateAntoraPlaybook task) {
		task.getAntoraExtensions().getZipContentsCollector().getDependencies().add(getName());
	}

	private void addPublishedMavenArtifact(MavenPublication mavenPublication, TaskProvider<?> copyAntoraContent) {
		if ("maven".equals(mavenPublication.getName())) {
			String classifier = "%s-%s-content".formatted(getName(), getType());
			mavenPublication.artifact(copyAntoraContent, (mavenArtifact) -> mavenArtifact.setClassifier(classifier));
		}
	}

	private RegularFile getContentZipFile(Directory dir) {
		Object version = getProject().getVersion();
		return dir.file("spring-boot-docs-%s-%s-%s-content.zip".formatted(version, getName(), getType()));
	}

	private static String toDescription(String input) {
		return input.replace("-", " ");
	}

	private Configuration createConfiguration(String name) {
		return getProject().getConfigurations()
			.create("%sAntora%sContent".formatted(toCamelCase(name), StringUtils.capitalize(getType())));
	}

	static abstract class CopyAntoraContent extends DefaultTask {

		private FileCollection source;

		@Inject
		public CopyAntoraContent() {

		}

		@InputFiles
		public FileCollection getSource() {
			return this.source;
		}

		public void setSource(FileCollection source) {
			this.source = source;
		}

		@OutputFile
		public abstract RegularFileProperty getOutputFile();

		@TaskAction
		void copyAntoraContent() throws IllegalStateException, IOException {
			Files.copy(this.source.getSingleFile().toPath(), getOutputFile().getAsFile().get().toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}

	}

}
