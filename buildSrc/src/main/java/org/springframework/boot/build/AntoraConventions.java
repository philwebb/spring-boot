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

import io.spring.gradle.antora.GenerateAntoraYmlPlugin;
import io.spring.gradle.antora.GenerateAntoraYmlTask;
import org.antora.gradle.AntoraExtension;
import org.antora.gradle.AntoraPlugin;
import org.antora.gradle.AntoraTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

import org.springframework.boot.build.artifacts.ArtifactRelease;

/**
 * Conventions that are applied in the presence of the {@link AntoraPlugin} and
 * {@link GenerateAntoraYmlPlugin}.
 *
 * @author Phillip Webb
 */
public class AntoraConventions {

	private static final String ANTORA_VERSION = "3.2.0-alpha.4";

	private static final String ANTORA_SOURCE = "src/docs/antora";

	private static final Map<String, String> PACKAGES;
	static {
		Map<String, String> packages = new LinkedHashMap<>();
		packages.put("@asciidoctor/tabs", "1.0.0-beta.6");
		packages.put("@springio/antora-extensions", "1.8.2");
		packages.put("@springio/asciidoctor-extensions", "1.0.0-alpha.9");
		PACKAGES = Collections.unmodifiableMap(packages);
	}

	void apply(Project project) {
		project.getPlugins().withType(AntoraPlugin.class, (antoraPlugin) -> apply(project, antoraPlugin));
	}

	private void apply(Project project, AntoraPlugin antoraPlugin) {
		project.getPlugins().apply(GenerateAntoraYmlPlugin.class);
		TaskContainer tasks = project.getTasks();
		tasks.withType(GenerateAntoraYmlTask.class,
				(generateAntoraYmlTask) -> configureGenerateAntoraYmlTask(project, generateAntoraYmlTask));
		tasks.withType(AntoraTask.class, (antoraTask) -> configureAntoraTask(project, antoraTask));
		configureAntoraExtension(project.getExtensions().getByType(AntoraExtension.class));
	}

	private void configureGenerateAntoraYmlTask(Project project, GenerateAntoraYmlTask generateAntoraYmlTask) {
		generateAntoraYmlTask.setProperty("baseAntoraYmlFile", project.file(ANTORA_SOURCE + "/antora.yml"));
		generateAntoraYmlTask.setProperty("componentName", "spring-boot");
		generateAntoraYmlTask.setProperty("outputFile",
				new File(project.getBuildDir(), "generated/docs/antora-yml/antora.yml"));
		generateAntoraYmlTask.doFirst((task) -> generateAntoraYmlTask.getAsciidocAttributes()
			.putAll(project.provider(() -> provideAsciidocAttributes(project))));
	}

	private Map<String, Object> provideAsciidocAttributes(Project project) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		ArtifactRelease artifacts = ArtifactRelease.forProject(project);
		attributes.put("github-tag", determineGitHubTag(project));
		attributes.put("artifact-release-type", artifacts.getType());
		attributes.put("url-artifact-repository", artifacts.getDownloadRepo());
		return attributes;
	}

	private String determineGitHubTag(Project project) {
		String version = "v" + project.getVersion();
		return (version.endsWith("-SNAPSHOT")) ? "main" : version;
	}

	private void configureAntoraTask(Project project, AntoraTask antoraTask) {
	}

	private void configureAntoraExtension(AntoraExtension antoraExtension) {
		antoraExtension.getVersion().convention(ANTORA_VERSION);
		antoraExtension.getPackages().convention(PACKAGES);
		antoraExtension.getOptions().addAll("--log-level", "all", "--stacktrace");
	}

}
