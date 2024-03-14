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

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.Project;

import org.springframework.boot.build.artifacts.ArtifactRelease;

/**
 * @author pwebb
 */
public class AntoraAsciidocAttributes {

	private static final String DASH_SNAPSHOT = "-SNAPSHOT";

	private final String version;

	private final boolean latestVersion;

	private final ArtifactRelease artifactRelease;

	public AntoraAsciidocAttributes(Project project) {
		this.version = String.valueOf(project.getVersion());
		this.latestVersion = Boolean.valueOf(String.valueOf(project.findProperty("latestVersion")));
		this.artifactRelease = ArtifactRelease.forProject(project);
	}

	AntoraAsciidocAttributes(String version, boolean latestVersion, ArtifactRelease artifactRelease) {
		this.version = version;
		this.latestVersion = latestVersion;
		this.artifactRelease = artifactRelease;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = new LinkedHashMap<>();
		// attributes.put("github-tag", determineGitHubTag(project));
		// attributes.put("artifact-release-type", this.artifactRelease.getType());
		// attributes.put("url-artifact-repository",
		// this.artifactRelease.getDownloadRepo());
		addGitHubAttributes(attributes);
		return attributes;
	}

	private void addGitHubAttributes(Map<String, String> attributes) {
		attributes.put("github-ref", determineGitHubRef());
	}

	private String determineGitHubRef() {
		int snapshotIndex = this.version.lastIndexOf(DASH_SNAPSHOT);
		if (snapshotIndex == -1) {
			return "v" + this.version;
		}
		if (this.latestVersion) {
			return "main";
		}
		String versionRoot = this.version.substring(0, snapshotIndex);
		int lastDot = versionRoot.lastIndexOf('.');
		return versionRoot.substring(0, lastDot) + "x";
	}

	private void addVersionAttributes(Map<String, String> attributes) {
	}

	private void addLinkAttributes(Map<String, String> attributes) {
	}

}
