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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
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

	private static final String XREF_EXTENSION = "/Users/pwebb/projects/antora-xref-extension/packages/antora-xref-extension";

	private static final String ZIP_CONTENTS_COLLECTOR_EXTENSION = "/Users/pwebb/projects/antora-zip-contents-collector-extension/packages/zip-contents-collector-extension";

	@OutputFile
	abstract RegularFileProperty getOutputFile();

	@Internal
	public abstract Property<String> getContentSourceConfiguration();

	@Internal
	public abstract ListProperty<String> getXrefStubs();

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
		return data;
	}

	private void addExtensions(Map<String, Object> data) throws IOException {
		List<Map<String, Object>> extensionsConfig = getList(data, "antora.extensions");
		extensionsConfig.add(createXrefExtensionConfig());
		extensionsConfig.add(createZipContentsCollectorExtensionConfig());
		List<Map<String, Object>> contentSources = getList(data, "content.sources");
		contentSources.add(createContentSource());
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
			config.put("version_file", "gradle.properties");
			Path location = getRelativeProjectPath().resolve("build/generated/docs/antora-content/"
					+ getProject().getName() + "-${version}-${name}-${classifier}.zip");
			config.put("locations", List.of(location.toString()));
		});
	}

	private Map<String, Object> createExtensionConfig(String require,
			ThrowingConsumer<Map<String, Object>> customizer) {
		Map<String, Object> config = new LinkedHashMap<>();
		config.put("require", require);
		customizer.accept(config);
		return config;
	}

	private Map<String, Object> createContentSource() throws IOException {
		Map<String, Object> source = new LinkedHashMap<>();
		Path rootProjectPath = getProject().getRootProject().getProjectDir().toPath().toRealPath();
		Path playbookPath = getOutputFile().get().getAsFile().toPath().getParent();
		Path antoraSrc = getProject().getLayout()
			.getProjectDirectory()
			.getAsFile()
			.toPath()
			.toRealPath()
			.resolve("src/docs/antora");
		StringBuilder url = new StringBuilder(".");
		rootProjectPath.relativize(playbookPath).normalize().forEach((path) -> url.append("/.."));
		source.put("url", url.toString());
		source.put("branches", "HEAD");
		List<String> startPaths = new ArrayList<>();
		startPaths.add(rootProjectPath.relativize(antoraSrc).toString());
		source.put("start_paths", startPaths);
		return source;
	}

	private void addSources(Map<String, Object> data) {
		getProject().getRootProject().getProjectDir();
		getProject().getProjectDir();

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadPlaybookTemplate() throws IOException {
		try (InputStream resource = getClass().getResourceAsStream("antora-playbook-template.yml")) {
			return createYaml().loadAs(resource, LinkedHashMap.class);
		}
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

	private Path getRelativeProjectPath() throws IOException {
		Path rootProjectPath = getProject().getRootProject().getProjectDir().toPath().toRealPath();
		Path projectPath = getProject().getProjectDir().toPath().toRealPath();
		return rootProjectPath.relativize(projectPath).normalize();
	}

}
