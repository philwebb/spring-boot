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

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;

/**
 * A contribution of content to Antora.
 *
 * @author Andy Wilkinson
 */
abstract class ContentContribution extends Contribution {

	private final Project project;

	private final String type;

	private final String name;

	protected ContentContribution(Project project, String type, String name) {
		this.project = project;
		this.type = type;
		this.name = name;
	}

	protected Project getProject() {
		return this.project;
	}

	protected String getType() {
		return this.type;
	}

	protected String getName() {
		return this.name;
	}

	abstract void produceFrom(CopySpec copySpec);

	protected TaskProvider<? extends Task> configureProduction(CopySpec copySpec) {
		TaskContainer tasks = this.project.getTasks();
		TaskProvider<Zip> zipContent = tasks.register(
				"zip%sAntora%sContent".formatted(toPascalCase(this.name), toPascalCase(this.type)), Zip.class,
				(zip) -> {
					zip.getDestinationDirectory()
						.set(this.project.getLayout().getBuildDirectory().dir("generated/docs/antora-content"));
					zip.getArchiveClassifier().set("%s-%s-content".formatted(this.name, this.type));
					zip.with(copySpec);
					zip.setDescription("Creates a zip archive of the %s Antora %s content.".formatted(this.name,
							toDescription(this.type)));
				});
		tasks.named("antora", (task) -> task.getInputs().files(zipContent).withPropertyName(zipContent.getName()));
		return zipContent;
	}

	private static String toDescription(String input) {
		return input.replace("-", " ");
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
