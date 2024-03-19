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
import java.util.List;
import java.util.Map;

import com.github.gradle.node.NodeExtension;
import io.spring.gradle.antora.GenerateAntoraYmlPlugin;
import io.spring.gradle.antora.GenerateAntoraYmlTask;
import org.antora.gradle.AntoraExtension;
import org.antora.gradle.AntoraPlugin;
import org.antora.gradle.AntoraTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;

import org.springframework.boot.build.antora.AntoraAsciidocAttributes;
import org.springframework.boot.build.antora.GenerateAntoraPlaybook;
import org.springframework.boot.build.bom.BomExtension;
import org.springframework.boot.build.constraints.ExtractVersionConstraints;
import org.springframework.util.Assert;

/**
 * Conventions that are applied in the presence of the {@link AntoraPlugin} and
 * {@link GenerateAntoraYmlPlugin}.
 *
 * @author Phillip Webb
 */
public class AntoraConventions {

	private static final String DEPENDENCIES_PATH = ":spring-boot-project:spring-boot-dependencies";

	private static final String ANTORA_VERSION = "3.2.0-alpha.4";

	private static final String ANTORA_SOURCE_DIR = "src/docs/antora";

	private static final List<String> NAV_FILES = List.of("nav.adoc", "local-nav.adoc");

	private static final Map<String, String> PACKAGES;
	static {
		Map<String, String> packages = new LinkedHashMap<>();
		packages.put("@asciidoctor/tabs", "1.0.0-beta.6");
		packages.put("@springio/antora-extensions", "1.8.2");
		packages.put("@springio/asciidoctor-extensions", "1.0.0-alpha.10");
		PACKAGES = Collections.unmodifiableMap(packages);
	}

	void apply(Project project) {
		project.getPlugins().withType(AntoraPlugin.class, (antoraPlugin) -> apply(project, antoraPlugin));
	}

	private void apply(Project project, AntoraPlugin antoraPlugin) {
		ExtractVersionConstraints dependencyVersionsTask = addDependencyVersionsTask(project);
		project.getPlugins().apply(GenerateAntoraYmlPlugin.class);
		TaskContainer tasks = project.getTasks();
		GenerateAntoraPlaybook generateAntoraPlaybookTask = tasks.create("generateAntoraPlaybook",
				GenerateAntoraPlaybook.class);
		tasks.withType(GenerateAntoraYmlTask.class, (generateAntoraYmlTask) -> configureGenerateAntoraYmlTask(project,
				generateAntoraYmlTask, dependencyVersionsTask));
		tasks.withType(AntoraTask.class,
				(antoraTask) -> configureAntoraTask(project, antoraTask, generateAntoraPlaybookTask));
		project.getExtensions()
			.configure(AntoraExtension.class, (antoraExtension) -> configureAntoraExtension(antoraExtension,
					generateAntoraPlaybookTask.getOutputFile()));
		project.getExtensions()
			.configure(NodeExtension.class, (nodeExtension) -> configureNodeExtension(project, nodeExtension));
	}

	private ExtractVersionConstraints addDependencyVersionsTask(Project project) {
		return project.getTasks()
			.create("dependencyVersions", ExtractVersionConstraints.class,
					(task) -> task.enforcedPlatform(DEPENDENCIES_PATH));
	}

	private void configureGenerateAntoraYmlTask(Project project, GenerateAntoraYmlTask generateAntoraYmlTask,
			ExtractVersionConstraints dependencyVersionsTask) {
		generateAntoraYmlTask.getOutputs().doNotCacheIf("getAsciidocAttributes() changes output", (task) -> true);
		generateAntoraYmlTask.dependsOn(dependencyVersionsTask);
		generateAntoraYmlTask.setProperty("componentName", "spring-boot");
		generateAntoraYmlTask.setProperty("outputFile",
				new File(project.getBuildDir(), "generated/docs/antora-yml/antora.yml"));
		generateAntoraYmlTask.setProperty("yml", getDefaultYml(project));
		generateAntoraYmlTask.doFirst((task) -> generateAntoraYmlTask.getAsciidocAttributes()
			.putAll(project.provider(() -> getAsciidocAttributes(project, dependencyVersionsTask))));
	}

	private Map<String, ?> getDefaultYml(Project project) {
		String navFile = null;
		for (String candidate : NAV_FILES) {
			if (project.file(ANTORA_SOURCE_DIR + "/" + candidate).exists()) {
				Assert.state(navFile == null, "Multiple nav files found");
				navFile = candidate;
			}
		}
		Map<String, Object> defaultYml = new LinkedHashMap<>();
		defaultYml.put("title", "Spring Boot");
		if (navFile != null) {
			defaultYml.put("nav", List.of(navFile));
		}
		return defaultYml;
	}

	private Map<String, String> getAsciidocAttributes(Project project,
			ExtractVersionConstraints dependencyVersionsTask) {
		BomExtension bom = (BomExtension) project.project(DEPENDENCIES_PATH).getExtensions().getByName("bom");
		Map<String, String> dependencyVersions = dependencyVersionsTask.getVersionConstraints();
		AntoraAsciidocAttributes attributes = new AntoraAsciidocAttributes(project, bom, dependencyVersions);
		return attributes.get();
	}

	private void configureAntoraTask(Project project, AntoraTask antoraTask,
			GenerateAntoraPlaybook generateAntoraPlaybookTask) {
		antoraTask.setGroup("Documentation");
		antoraTask.getDependsOn().add(generateAntoraPlaybookTask);
		project.getPlugins()
			.withType(JavaBasePlugin.class,
					(javaBasePlugin) -> project.getTasks()
						.getByName(JavaBasePlugin.CHECK_TASK_NAME)
						.dependsOn(antoraTask));
	}

	private void configureAntoraExtension(AntoraExtension antoraExtension, Provider<RegularFile> playbook) {
		antoraExtension.getVersion().convention(ANTORA_VERSION);
		antoraExtension.getPackages().convention(PACKAGES);
		antoraExtension.getPlaybook().convention(playbook.map(RegularFile::getAsFile));
	}

	private void configureNodeExtension(Project project, NodeExtension nodeExtension) {
		File rootBuildDir = project.getRootProject().getBuildDir();
		nodeExtension.getWorkDir().set(rootBuildDir.toPath().resolve(".gradle/nodejs").toFile());
		nodeExtension.getNpmWorkDir().set(rootBuildDir.toPath().resolve(".gradle/npm").toFile());
	}

}
