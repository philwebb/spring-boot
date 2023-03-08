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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.spring.gradle.antora.GenerateAntoraYmlPlugin;
import org.antora.gradle.AntoraExtension;
import org.antora.gradle.AntoraPlugin;
import org.gradle.api.Project;

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
		project.getPlugins().withType(AntoraPlugin.class, (plugin) -> apply(project, plugin));
		project.getPlugins().withType(GenerateAntoraYmlPlugin.class, (plugin) -> apply(project, plugin));
	}

	private void apply(Project project, AntoraPlugin plugin) {
		AntoraExtension antoraExtension = project.getExtensions().getByType(AntoraExtension.class);
		antoraExtension.getVersion().convention(ANTORA_VERSION);
		antoraExtension.getPackages().convention(PACKAGES);
	}

	private void apply(Project project, GenerateAntoraYmlPlugin plugin) {
	}

}
