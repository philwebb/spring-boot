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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.repositories.PasswordCredentials;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringRepositoriesExtension}.
 *
 * @author Phillip Webb
 */
class SpringRepositoriesExtensionTests {

	private final List<MavenArtifactRepository> repositories = new ArrayList<>();

	private final List<RepositoryContentDescriptor> contents = new ArrayList<>();

	private final List<PasswordCredentials> credentials = new ArrayList<>();

	@Test
	void mavenRepositoriesWhenNotCommercial() {
		SpringRepositoriesExtension extension = createExtension(false);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(2);
		verify(this.repositories.get(0)).setName("spring-oss-milestone");
		verify(this.repositories.get(0)).setUrl("https://repo.spring.io/milestone");
		verify(this.repositories.get(1)).setName("spring-oss-snapshot");
		verify(this.repositories.get(1)).setUrl("https://repo.spring.io/snapshot");
	}

	@Test
	void mavenRepositoriesWhenCommercial() {
		SpringRepositoriesExtension extension = createExtension(true);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setName("spring-commercial-release");
		verify(this.repositories.get(0))
			.setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-prod-local");
		verify(this.repositories.get(1)).setName("spring-oss-milestone");
		verify(this.repositories.get(1)).setUrl("https://repo.spring.io/milestone");
		verify(this.repositories.get(2)).setName("spring-commercial-snapshot");
		verify(this.repositories.get(2)).setUrl("https://usw1.packages.broadcom.com/spring-enterprise-maven-dev-local");
		verify(this.repositories.get(3)).setName("spring-oss-snapshot");
		verify(this.repositories.get(3)).setUrl("https://repo.spring.io/snapshot");
	}

	@Test
	void mavenRepositoriesWhenConditionMatches() {
		SpringRepositoriesExtension extension = createExtension(false);
		extension.mavenRepositories(true);
		assertThat(this.repositories).hasSize(2);
	}

	@Test
	void mavenRepositoriesWhenConditionDoesNotMatch() {
		SpringRepositoriesExtension extension = createExtension(false);
		extension.mavenRepositories(false);
		assertThat(this.repositories).isEmpty();
	}

	@Test
	void mavenRepositoriesExcludingBootGroup() {
		SpringRepositoriesExtension extension = createExtension(false);
		extension.mavenRepositoriesExcludingBootGroup();
		assertThat(this.contents).hasSize(2);
		verify(this.contents.get(0)).excludeGroup("org.springframework.boot");
		verify(this.contents.get(1)).excludeGroup("org.springframework.boot");
	}

	@Test
	void mavenRepositoriesWithRepositorySpecificEnvironmentVariables() {
		Map<String, String> environment = new HashMap<>();
		environment.put("COMMERCIAL_RELEASE_REPO_URL", "curl");
		environment.put("COMMERCIAL_RELEASE_REPO_USERNAME", "cuser");
		environment.put("COMMERCIAL_RELEASE_REPO_PASSWORD", "cpass");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_URL", "surl");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_USERNAME", "suser");
		environment.put("COMMERCIAL_SNAPSHOT_REPO_PASSWORD", "spass");
		SpringRepositoriesExtension extension = createExtension(true, environment::get);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setUrl("curl");
		verify(this.repositories.get(2)).setUrl("surl");
		assertThat(this.credentials).hasSize(2);
		verify(this.credentials.get(0)).setUsername("cuser");
		verify(this.credentials.get(0)).setPassword("cpass");
		verify(this.credentials.get(1)).setUsername("suser");
		verify(this.credentials.get(1)).setPassword("spass");
	}

	@Test
	void mavenRepositoriesWhenRepositoryEnvironmentVariables() {
		Map<String, String> environment = new HashMap<>();
		environment.put("COMMERCIAL_REPO_URL", "url");
		environment.put("COMMERCIAL_REPO_USERNAME", "user");
		environment.put("COMMERCIAL_REPO_PASSWORD", "pass");
		SpringRepositoriesExtension extension = createExtension(true, environment::get);
		extension.mavenRepositories();
		assertThat(this.repositories).hasSize(4);
		verify(this.repositories.get(0)).setUrl("url");
		verify(this.repositories.get(2)).setUrl("url");
		assertThat(this.credentials).hasSize(2);
		verify(this.credentials.get(0)).setUsername("user");
		verify(this.credentials.get(0)).setPassword("pass");
		verify(this.credentials.get(1)).setUsername("user");
		verify(this.credentials.get(1)).setPassword("pass");
	}

	private SpringRepositoriesExtension createExtension(boolean commercial) {
		return createExtension(commercial, (name) -> null);
	}

	@SuppressWarnings({ "unchecked", "unchecked" })
	private SpringRepositoriesExtension createExtension(boolean commercial, UnaryOperator<String> environment) {
		Project project = mock(Project.class);
		RepositoryHandler repositoryHandler = mock(RepositoryHandler.class);
		given(project.findProperty("spring.build-type")).willReturn(!commercial ? "oss" : "commercial");
		given(project.getRepositories()).willReturn(repositoryHandler);
		given(repositoryHandler.maven(any(Action.class))).willAnswer((Answer<?>) this::mavenAction);
		return new SpringRepositoriesExtension(project, environment);
	}

	@SuppressWarnings({ "unchecked", "unchecked" })
	private Object mavenAction(InvocationOnMock invocation) {
		MavenArtifactRepository repository = mock(MavenArtifactRepository.class);
		willAnswer((Answer<?>) this::contentAction).given(repository).content(any(Action.class));
		willAnswer((Answer<?>) this::credentialsAction).given(repository).credentials(any(Action.class));
		Action<MavenArtifactRepository> action = invocation.getArgument(0);
		action.execute(repository);
		this.repositories.add(repository);
		return null;
	}

	private Object contentAction(InvocationOnMock invocation) {
		RepositoryContentDescriptor content = mock(RepositoryContentDescriptor.class);
		Action<RepositoryContentDescriptor> action = invocation.getArgument(0);
		action.execute(content);
		this.contents.add(content);
		return null;
	}

	private Object credentialsAction(InvocationOnMock invocation) {
		PasswordCredentials credentials = mock(PasswordCredentials.class);
		Action<PasswordCredentials> action = invocation.getArgument(0);
		action.execute(credentials);
		this.credentials.add(credentials);
		return null;
	}

}
