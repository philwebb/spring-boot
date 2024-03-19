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

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskAction;

import org.springframework.util.ReflectionUtils;

/**
 * I do not know.
 *
 * @author Phillip Webb
 */
public class Dunno extends DefaultTask {

	@TaskAction
	void doIt() {
		if (true) {
			return;
		}
		getProject().getPluginManager().apply(PublishingPlugin.class);
		PublishingExtension publishingExtension = getProject().getExtensions().getByType(PublishingExtension.class);
		System.out.println(publishingExtension.getClass());
		ReflectionUtils.doWithMethods(publishingExtension.getClass(), (m) -> System.out.println(m));

		MavenPublication mavenPublication = (MavenPublication) publishingExtension.getPublications().getByName("maven");
		Configuration antoraContent = getProject().getConfigurations().findByName("antoraContent");
		antoraContent.getDependencies().forEach((dependency) -> {
			ProjectDependency projectDependency = (ProjectDependency) dependency;
			Configuration targetConfiguration = projectDependency.getDependencyProject()
				.getConfigurations()
				.getByName(projectDependency.getTargetConfiguration());
			targetConfiguration.getArtifacts().forEach((artifact) -> {
				System.out.println(artifact);
				mavenPublication.artifact(artifact);
			});
		});

	}

	void xdoit() {
		Configuration configuration = getProject().getConfigurations().findByName("antoraContent");
		configuration.getDependencies().forEach((dep) -> {
			ProjectDependency projectDependency = (ProjectDependency) dep;
			System.out.println("&&&");
			System.out.println(dep);
			System.out.println(dep.getClass());
			System.out.println(projectDependency.getTargetConfiguration());
			projectDependency.getArtifacts().forEach(System.out::println);
			Configuration target = projectDependency.getDependencyProject()
				.getConfigurations()
				.getByName(projectDependency.getTargetConfiguration());
			System.out.println(target);
			System.out.println(target.getArtifacts());
			target.getArtifacts().forEach(System.out::println);
		});

		PublishingExtension extension = getProject().getExtensions().getByType(PublishingExtension.class);
		extension.getPublications().getByName("maven", (pub) -> {
		});
	}

}
