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

package org.springframework.boot.build.settings;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/**
 * Extension added to the {@link RepositoryHandler} to provide support for Spring specific
 * Maven repositories.
 *
 * @author Phillip Webb
 */
public class SpringRepositoriesExtension {

	// FIXME only if version matches

	private static final Consumer<MavenArtifactRepository> NO_ACTION = (maven) -> {
	};

	private final Project project;

	private final UnaryOperator<String> environment;

	@Inject
	public SpringRepositoriesExtension(Project project) {
		this(project, System::getenv);
	}

	SpringRepositoriesExtension(Project project, UnaryOperator<String> environment) {
		this.project = project;
		this.environment = environment;
	}

	void mavenRepositories() {
		addRepositories(NO_ACTION);
	}

	void mavenRepositories(boolean condition) {
		if (condition) {
			addRepositories(NO_ACTION);
		}
	}

	void mavenRepositoriesExcludingBootGroup() {
		addRepositories((maven) -> maven.content((content) -> {
			content.excludeGroup("org.springframework.boot");
		}));
	}

	private void addRepositories(Consumer<MavenArtifactRepository> action) {
		addCommercialRepository("release", "/spring-enterprise-maven-prod-local", action);
		addOssRepository("milestone", "/milestone", action);
		addCommercialRepository("snapshot", "/spring-enterprise-maven-dev-local", action);
		addOssRepository("snapshot", "/snapshot", action);
	}

	private void addOssRepository(String id, String path, Consumer<MavenArtifactRepository> action) {
		String name = "spring-oss-" + id;
		String url = "https://repo.spring.io" + path;
		addRepository(name, url, action);
	}

	private void addCommercialRepository(String id, String path, Consumer<MavenArtifactRepository> action) {
		if (!"commercial".equalsIgnoreCase((String) this.project.findProperty("spring.build-type"))) {
			return;
		}
		String name = "spring-commercial-" + id;
		String url = fromEnv("COMMERCIAL_%SREPO_URL", id, "https://usw1.packages.broadcom.com" + path);
		String username = fromEnv("COMMERCIAL_%SREPO_USERNAME", id);
		String password = fromEnv("COMMERCIAL_%SREPO_PASSWORD", id);
		addRepository(name, url, (maven) -> {
			maven.credentials((credentials) -> {
				credentials.setUsername(username);
				credentials.setPassword(password);
			});
			action.accept(maven);
		});
	}

	private void addRepository(String name, String url, Consumer<MavenArtifactRepository> action) {
		this.project.getRepositories().maven((maven) -> {
			maven.setName(name);
			maven.setUrl(url);
			action.accept(maven);
		});
	}

	private String fromEnv(String template, String id) {
		return fromEnv(template, id, null);
	}

	private String fromEnv(String template, String id, String defaultValue) {
		String value = this.environment.apply(template.formatted(id.toUpperCase() + "_"));
		value = (value != null) ? value : this.environment.apply(template.formatted(""));
		return (value != null) ? value : defaultValue;
	}

}
