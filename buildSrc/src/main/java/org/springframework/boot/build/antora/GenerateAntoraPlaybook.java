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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.util.function.ThrowingConsumer;

/**
 * Task to generate a local Antora playbook.
 *
 * @author Phillip Webb
 */
public abstract class GenerateAntoraPlaybook extends DefaultTask {

	private static final String ANTORA_SOURCE_DIR = "src/docs/antora";

	private static final String XREF_EXTENSION = "@springio/antora-xref-extension";

	private static final String ZIP_CONTENTS_COLLECTOR_EXTENSION = "@springio/antora-zip-contents-collector-extension";

	private static final String ROOT_COMPONENT_EXTENSION = "@springio/antora-extensions/root-component-extension";

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	@Input
	public abstract Property<String> getContentSourceConfiguration();

	@Input
	@Optional
	public abstract ListProperty<String> getXrefStubs();

	@Input
	@Optional
	public abstract MapProperty<String, String> getAlwaysInclude();

	public GenerateAntoraPlaybook() {
		setGroup("Documentation");
		setDescription("Generates an Antora playbook.yml file for local use");
		getOutputFile().convention(getProject().getLayout()
			.getBuildDirectory()
			.file("generated/docs/antora-playbook/antora-playbook.yml"));
		getContentSourceConfiguration().convention("antoraContent");
	}

	@TaskAction
	public void writePlaybookYml() throws IOException {
		File file = getOutputFile().get().getAsFile();
		file.getParentFile().mkdirs();
		try (FileWriter out = new FileWriter(file)) {
			createYaml().dump(getData(), out);
		}
	}

	@Input
	final Map<String, Object> getData() throws IOException {
		Map<String, Object> data = loadPlaybookTemplate();
		addExtensions(data);
		addSources(data);
		addDir(data);
		return data;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadPlaybookTemplate() throws IOException {
		try (InputStream resource = getClass().getResourceAsStream("antora-playbook-template.yml")) {
			return createYaml().loadAs(resource, LinkedHashMap.class);
		}
	}

	@SuppressWarnings("unchecked")
	private void addExtensions(Map<String, Object> data) {
		List<Map<String, Object>> extensionsConfig = new ArrayList<>();
		extensionsConfig.add(createXrefExtensionConfig());
		extensionsConfig.add(createZipContentsCollectorExtensionConfig());
		extensionsConfig.add(createRootComponentExtensionConfig()); // Must be last
		Map<String, Object> antora = (Map<String, Object>) data.get("antora");
		antora.put("extensions", extensionsConfig);
	}

	private Map<String, Object> createXrefExtensionConfig() {
		return createExtensionConfig(XREF_EXTENSION, (config) -> {
			List<String> xrefStubs = getXrefStubs().getOrElse(Collections.emptyList());
			if (!xrefStubs.isEmpty()) {
				config.put("stub", xrefStubs);
			}
		});
	}

	private Map<String, Object> createZipContentsCollectorExtensionConfig() {
		return createExtensionConfig(ZIP_CONTENTS_COLLECTOR_EXTENSION, (config) -> {
			Map<String, String> alwaysInclude = getAlwaysInclude().getOrNull();
			config.put("version_file", "gradle.properties");
			Path location = getRelativeProjectPath().resolve("build/generated/docs/antora-content/"
					+ getProject().getName() + "-${version}-${name}-${classifier}.zip");
			config.put("locations", List.of(location.toString()));
			if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
				config.put("always_include", List.of(alwaysInclude));
			}
		});
	}

	private Map<String, Object> createRootComponentExtensionConfig() {
		return createExtensionConfig(ROOT_COMPONENT_EXTENSION,
				(config) -> config.put("root_component_name", "spring-boot"));
	}

	private Map<String, Object> createExtensionConfig(String require,
			ThrowingConsumer<Map<String, Object>> customizer) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("require", require);
		customizer.accept(config);
		return config;
	}

	private void addSources(Map<String, Object> data) {
		List<Map<String, Object>> contentSources = getList(data, "content.sources");
		contentSources.add(createContentSource());
	}

	private Map<String, Object> createContentSource() {
		Map<String, Object> source = new LinkedHashMap<>();
		Path playbookPath = getOutputFile().get().getAsFile().toPath().getParent();
		Path antoraSrc = getProjectPath(getProject()).resolve(ANTORA_SOURCE_DIR);
		StringBuilder url = new StringBuilder(".");
		relativizeFromRootProject(playbookPath).normalize().forEach((path) -> url.append("/.."));
		source.put("url", url.toString());
		source.put("branches", "HEAD");
		source.put("version", getProject().getVersion().toString());
		Set<String> startPaths = new LinkedHashSet<>();
		addAntoraContentStartPaths(startPaths);
		startPaths.add(relativizeFromRootProject(antoraSrc).toString());
		source.put("start_paths", startPaths.stream().toList());
		return source;
	}

	private void addAntoraContentStartPaths(Set<String> startPaths) {
		Configuration configuration = getProject().getConfigurations().findByName("antoraContent");
		if (configuration != null) {
			for (ProjectDependency dependency : configuration.getAllDependencies().withType(ProjectDependency.class)) {
				Path path = dependency.getDependencyProject().getProjectDir().toPath();
				startPaths.add(relativizeFromRootProject(path).resolve(ANTORA_SOURCE_DIR).toString());
			}
		}
	}

	private void addDir(Map<String, Object> data) {
		Path playbookDir = toRealPath(getOutputFile().get().getAsFile().toPath()).getParent();
		Path outputDir = toRealPath(getProject().getBuildDir().toPath().resolve("site"));
		data.put("output", Map.of("dir", "./" + playbookDir.relativize(outputDir).toString()));
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getList(Map<String, Object> data, String location) {
		return (List<T>) get(data, location);
	}

	@SuppressWarnings("unchecked")
	private Object get(Map<String, Object> data, String location) {
		Object result = data;
		String[] keys = location.split("\\.");
		for (String key : keys) {
			result = ((Map<String, Object>) result).get(key);
		}
		return result;
	}

	private Yaml createYaml() {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		return new Yaml(options);
	}

	private Path getRelativeProjectPath() {
		return relativizeFromRootProject(getProjectPath(getProject()));
	}

	private Path relativizeFromRootProject(Path subPath) {
		Path rootProjectPath = getProjectPath(getProject().getRootProject());
		return rootProjectPath.relativize(subPath).normalize();
	}

	private Path getProjectPath(Project project) {
		return toRealPath(project.getProjectDir().toPath());
	}

	private Path toRealPath(Path path) {
		try {
			return Files.exists(path) ? path.toRealPath() : path;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
