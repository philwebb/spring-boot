/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.docs;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.DocsType;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.JavadocLink;

/**
 * An {@link Action} to configure the links option of a {@link Javadoc} task.
 *
 * @author Andy Wilkinson
 */
public class ConfigureJavadocLinks implements Action<Javadoc> {

	private final FileCollection resolvedBoms;

	private final Collection<String> includedLibraries;

	private FileCollection javadocJars;

	public ConfigureJavadocLinks(Project project, Collection<String> includedLibraries) {
		ConfigurationContainer configurations = project.getConfigurations();
		ObjectFactory objects = project.getObjects();
		this.resolvedBoms = configurations.getByName("resolvedBom");
		ArtifactView artifactView = configurations.getByName("javadocMacros").getIncoming().artifactView((view) -> {
			view.withVariantReselection().attributes((attributes) -> {
				attributes.attribute(Category.CATEGORY_ATTRIBUTE,
						objects.named(Category.class, Category.DOCUMENTATION));
				attributes.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.class, DocsType.JAVADOC));
			});
		});
		this.javadocJars = artifactView.getArtifacts().getArtifactFiles();
		this.includedLibraries = includedLibraries;
	}

	@Override
	public void execute(Javadoc javadoc) {
		javadoc.options((options) -> {
			if (options instanceof StandardJavadocDocletOptions standardOptions) {
				configureLinks(standardOptions);
			}
		});
	}

	private void configureLinks(StandardJavadocDocletOptions options) {
		ResolvedBom resolvedBom = ResolvedBom.readFrom(this.resolvedBoms.getSingleFile());
		resolvedBom.libraries().forEach((library) -> {
			System.out.println(library.name());
			for (Id id : library.managedDependencies()) {
				System.out.println(id);
			}
			library.links().javadoc().forEach((javadoc) -> System.out.println(javadoc.uri()));
		});
		for (File file : this.javadocJars) {
			System.out.println(file);
		}
		System.out.println("----");
		List<String> links = new ArrayList<>();
		links.add("https://docs.oracle.com/en/java/javase/17/docs/api/");
		links.add("https://jakarta.ee/specifications/platform/11/apidocs/");
		resolvedBom.libraries()
			.stream()
			.filter((candidate) -> this.includedLibraries.contains(candidate.name()))
			.flatMap((library) -> library.links().javadoc().stream())
			.map(JavadocLink::uri)
			.map(URI::toString)
			.forEach(links::add);
		options.setLinks(links);
	}

	// def view = configurations.javadocMacros.incoming.artifactView {
	// withVariantReselection()
	// attributes {
	// attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category,
	// Category.DOCUMENTATION))
	// attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType, DocsType.JAVADOC))
	// }
	// }
	//
	// def javadocFiles = view.artifacts.artifactFiles

}
