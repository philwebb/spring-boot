/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.boot.build;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import io.spring.gradle.antora.GenerateAntoraYmlPlugin;
import io.spring.gradle.antora.GenerateAntoraYmlTask;
import org.antora.gradle.AntoraExtension;
import org.antora.gradle.AntoraPlugin;
import org.antora.gradle.AntoraTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.Sync;

import org.springframework.boot.build.artifacts.ArtifactRelease;
import org.springframework.util.StringUtils;

/**
 * Conventions that are applied in the presence of the {@link AntoraPlugin} and
 * {@link GenerateAntoraYmlPlugin}.
 *
 * @author Phillip Webb
 */
public class AntoraConventions {

	private static final String ANTORA_VERSION = "3.2.0-alpha.2";

	private static final Map<String, String> PACKAGES;
	static {
		Map<String, String> packages = new LinkedHashMap<>();
		packages.put("@antora/atlas-extension", "1.0.0-alpha.1");
		packages.put("@antora/collector-extension", "1.0.0-alpha.2");
		packages.put("@antora/collector-extension", "1.0.0-alpha.3");
		packages.put("@asciidoctor/tabs", "1.0.0-alpha.12");
		packages.put("@opendevise/antora-release-line-extension", "1.0.0-alpha.2");
		packages.put("@springio/antora-extensions", "1.1.0");
		packages.put("@springio/asciidoctor-extensions", "1.0.0-alpha.9");
		PACKAGES = Collections.unmodifiableMap(packages);
	}

	void apply(Project project) {
		project.getPlugins()
			.withType(AntoraPlugin.class, (antoraPlugin) -> configureAntoraPlugin(project, antoraPlugin));
	}

	private void configureAntoraPlugin(Project project, AntoraPlugin antoraPlugin) {
		GenerateAntoraYmlPlugin generateAntoraYmlPlugin = project.getPlugins().getPlugin(GenerateAntoraYmlPlugin.class);
		Objects.requireNonNull(generateAntoraYmlPlugin, "GenerateAntoraYmlPlugin has not been applied");
		AntoraExtension extension = project.getExtensions().getByType(AntoraExtension.class);
		extension.getVersion().convention(ANTORA_VERSION);
		extension.getPackages().convention(PACKAGES);
		project.getTasks().withType(AntoraTask.class, (antoraTask) -> configureAntoraTask(project, antoraTask));
		configureGenerateAntoraYmlPlugin(project, generateAntoraYmlPlugin);
	}

	private void configureAntoraTask(Project project, AntoraTask antoraTask) {
		createSyncContentTask(project, antoraTask);
	}

	private Sync createSyncContentTask(Project project, AntoraTask antoraTask) {
		String taskName = "sync" + StringUtils.capitalize(antoraTask.getName()) + "Content";
		Sync syncTask = project.getTasks().create(taskName, Sync.class);
		File destination = new File(project.getBuildDir(), "generated/antora");
		syncTask.setDestinationDir(destination);
		project.getTasks().withType(GenerateAntoraYmlTask.class).forEach(syncTask::from);
		antoraTask.dependsOn(syncTask);
		antoraTask.getInputs()
			.dir(destination)
			.withPathSensitivity(PathSensitivity.RELATIVE)
			.withPropertyName("synced source");
		return syncTask;
	}

	private void configureGenerateAntoraYmlPlugin(Project project, GenerateAntoraYmlPlugin generateAntoraYmlPlugin) {
		project.getTasks()
			.withType(GenerateAntoraYmlTask.class,
					(generateAntoraYmlTask) -> configureGenerateAntoraYmlTask(project, generateAntoraYmlTask));
	}

	private void configureGenerateAntoraYmlTask(Project project, GenerateAntoraYmlTask generateAntoraYmlTask) {
		generateAntoraYmlTask.setProperty("baseAntoraYmlFile", project.file("src/docs/antora/antora.yml"));
		generateAntoraYmlTask.setProperty("outputFile",
				new File(project.getBuildDir(), "generated/antora-yml/antora.yml"));
		generateAntoraYmlTask.doFirst((actionedTask) -> generateAntoraYmlTask.getAsciidocAttributes()
			.putAll(project.provider(() -> provideAsciidocAttributes(project))));
	}

	private Map<String, Object> provideAsciidocAttributes(Project project) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		ArtifactRelease artifacts = ArtifactRelease.forProject(project);
		attributes.put("github-tag", determineGitHubTag(project));
		attributes.put("artifact-release-type", artifacts.getType());
		attributes.put("artifact-download-repo", artifacts.getDownloadRepo());
		return attributes;
	}

	private String determineGitHubTag(Project project) {
		String version = "v" + project.getVersion();
		return (version.endsWith("-SNAPSHOT")) ? "main" : version;
	}

}
