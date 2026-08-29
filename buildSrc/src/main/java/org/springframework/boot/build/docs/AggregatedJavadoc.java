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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.boot.build.bom.ResolvedBom.Id;
import org.springframework.boot.build.bom.ResolvedBom.JavadocLink;
import org.springframework.boot.build.bom.ResolvedBom.ResolvedLibrary;

/**
 * Specialized {@link Javadoc} task for aggregated javadoc generation.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public abstract class AggregatedJavadoc extends Javadoc {

	private static final Set<String> SKIPPED_LIBRARIES = Set.of("Spring Boot");

	private static final Set<String> JAVADOC_PACKAGE_LIST_FILES = Set.of("package-list", "element-list");

	@Classpath
	@InputFiles
	public abstract ConfigurableFileCollection getResolvedBom();

	@Classpath
	@InputFiles
	public abstract ConfigurableFileCollection getJavadocJars();

	@Override
	protected void generate() {
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) getOptions();
		options.doclet("io.spring.javaformat.doclet.OfflineLinksDoclet");
		options.addBooleanOption("quiet", true);
		options.addBooleanOption("Xdoclint:all,-missing", true);
		options.addBooleanOption("Werror", true);
		options.links("https://docs.oracle.com/en/java/javase/17/docs/api/");
		configureOfflineLinks(options);
		super.generate();
	}

	private void configureOfflineLinks(StandardJavadocDocletOptions options) {
		ResolvedBom resolvedBom = ResolvedBom.readFrom(getResolvedBom().getSingleFile());
		File packageListDirectory = getProject().getLayout()
			.getBuildDirectory()
			.get()
			.dir("docs/javadocpackagelist")
			.getAsFile();
		extractPackageListFiles(packageListDirectory);
		options.addStringOption("offlinelinks-source", new File(packageListDirectory, "@name@").getAbsolutePath());
		// options.addBooleanOption("offlinelinks-debug", true);
		for (ResolvedLibrary library : resolvedBom.libraries()) {
			System.out.println("## " + library.name());
			List<JavadocLink> javadocLinks = library.links().javadoc();
			Set<Id> allManagedDependencies = library.allManagedDependencies();
			System.out.println("** " + javadocLinks);
			System.out.println("** " + allManagedDependencies);
			if (isOfflineJavalinkedLibrary(library, javadocLinks, allManagedDependencies)) {
				JavadocLink javadocLink = javadocLinks.get(0);
				String url = javadocLink.uri().toString();
				String javadocJars = javadocJarNames(allManagedDependencies);
				System.out.println("  >> " + url + " " + javadocJars);
				options.linksOffline(url, javadocJars);
			}
		}
	}

	private boolean isOfflineJavalinkedLibrary(ResolvedLibrary library, List<JavadocLink> javadocLinks,
			Set<Id> allManagedDependencies) {
		return !SKIPPED_LIBRARIES.contains(library.name()) && javadocLinks.size() == 1
				&& !allManagedDependencies.isEmpty();
	}

	private String javadocJarNames(Set<Id> managedDependencies) {
		return managedDependencies.stream().map(this::javadocJarName).collect(Collectors.joining(","));
	}

	private String javadocJarName(Id managedDependency) {
		return "%s-%s-javadoc.jar".formatted(managedDependency.artifactId(), managedDependency.version());
	}

	private void extractPackageListFiles(File packageListDirectory) {
		getJavadocJars().forEach((javadocJar) -> {
			FileCollection source = getProject().zipTree(javadocJar).filter(this::isJavadocPackageListFile);
			File destination = new File(packageListDirectory, javadocJar.getName());
			getProject().copy((copy) -> copy.from(source).into(destination));
		});
	}

	private boolean isJavadocPackageListFile(File file) {
		return JAVADOC_PACKAGE_LIST_FILES.contains(file.getName());
	}

}
