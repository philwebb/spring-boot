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
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * A contribution of content to Antora that can be consumed by other projects.
 *
 * @author Andy Wilkinson
 */
class ConsumableContentContribution extends ContentContribution {

	protected ConsumableContentContribution(Project project, String type, String name) {
		super(project, type, name);
	}

	@Override
	void produceFrom(CopySpec copySpec) {
		TaskProvider<? extends Task> producer = super.configureProduction(copySpec);
		Configuration configuration = createConfiguration(getName());
		getProject().getArtifacts().add(configuration.getName(), producer);
	}

	void consumeFrom(String path) {
		Configuration configuration = createConfiguration(getName());
		getProject().getDependencies()
			.add(configuration.getName(), getProject().provider(() -> getProject().getDependencies()
				.project(Map.of("path", path, "configuration", configuration.getName()))));
		Provider<Directory> contentOutputDirectory = getProject().getLayout()
			.getBuildDirectory()
			.dir("generated/docs/antora-dependencies-content/" + getName());
		TaskContainer tasks = getProject().getTasks();
		TaskProvider<CopyAntoraContent> copyAntoraCatalogContent = tasks.register(
				"copy%sAntora%sContent".formatted(toPascalCase(getName()), toPascalCase(getType())),
				CopyAntoraContent.class, (task) -> {
					task.setSource(configuration);
					task.getOutputFile()
						.set(contentOutputDirectory.map((dir) -> dir.file("spring-boot-docs-%s-%s-%s-content.zip"
							.formatted(getProject().getVersion(), getName(), getType()))));
					task.setDescription("Syncs the %s Antora %s content from %s.".formatted(getName(),
							toDescription(getType()), path));
				});
		tasks.named("antora",
				(task) -> task.getInputs()
					.files(copyAntoraCatalogContent)
					.withPathSensitivity(PathSensitivity.RELATIVE)
					.withPropertyName(configuration.getName()));
		tasks.named("generateAntoraPlaybook", GenerateAntoraPlaybook.class,
				(task) -> task.getAntoraExtensions().getZipContentsCollector().getDependencies().add(getName()));
		getProject().getExtensions()
			.getByType(PublishingExtension.class)
			.getPublications()
			.withType(MavenPublication.class)
			.configureEach((mavenPublication) -> {
				if ("maven".equals(mavenPublication.getName())) {
					mavenPublication.artifact(copyAntoraCatalogContent, (mavenArtifact) -> mavenArtifact
						.setClassifier("%s-%s-content".formatted(getName(), getType())));
				}
			});
	}

	private static String toDescription(String input) {
		return input.replace("-", " ");
	}

	private Configuration createConfiguration(String name) {
		Configuration configuration = getProject().getConfigurations()
			.create("%sAntora%sContent".formatted(toCamelCase(name), StringUtils.capitalize(getType())));
		return configuration;
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
