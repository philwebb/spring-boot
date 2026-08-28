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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
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

	private static final Set<String> INCLUDED_LIBRARIES = Set.of("Spring Framework", "Spring Security", "Tomcat");

	private static final Set<String> JAVADOC_PACKAGE_LIST_FILES = Set.of("package-list", "element-list");

	private static final String JAVADOC_PACKAGE_LIST_DIRECTORY = "docs/javadocpackagelist";

	private static final List<String> LINKS;
	static {
		List<String> links = new ArrayList<>();
		links.add("https://docs.oracle.com/en/java/javase/17/docs/api/");
		links.add("https://jakarta.ee/specifications/platform/11/apidocs/");
		LINKS = Collections.unmodifiableList(links);
	}

	@Classpath
	@InputFiles
	public abstract ConfigurableFileCollection getResolvedBom();

	@Classpath
	@InputFiles
	public abstract ConfigurableFileCollection getJavadocClasspath();

	@Override
	protected void generate() {
		ResolvedBom resolvedBom = ResolvedBom.readFrom(getResolvedBom().getSingleFile());
		getJavadocClasspath().forEach(this::copyJavadocPackageList);
		String offlineLinksSource = getProject().getLayout()
			.getBuildDirectory()
			.get()
			.dir(JAVADOC_PACKAGE_LIST_DIRECTORY)
			.file("@name@")
			.getAsFile()
			.getAbsolutePath();
		StandardJavadocDocletOptions options = (StandardJavadocDocletOptions) getOptions();
		options.doclet("io.spring.javaformat.doclet.OfflineLinksDoclet");
		options.addBooleanOption("quiet", true);
		options.addBooleanOption("Xdoclint:all,-missing", true);
		options.addBooleanOption("Werror", true);
		options.links(LINKS.toArray(String[]::new));
		options.addStringOption("offlinelinks-source", offlineLinksSource);
		for (ResolvedLibrary library : resolvedBom.libraries()) {
			for (JavadocLink javadocLink : library.links().javadoc()) {
				javadocLink.uri().toString();
				for (Id managedDependency : library.managedDependencies()) {
					String artifactId = managedDependency.artifactId();
					String version = managedDependency.version();
					String directory = "%s-%s-javadoc.jar".formatted(artifactId, version);
				}
			}
		}
		super.generate();
	}

	private void copyJavadocPackageList(File javadocJarFile) {
		Directory destination = getProject().getLayout()
			.getBuildDirectory()
			.get()
			.dir(JAVADOC_PACKAGE_LIST_DIRECTORY)
			.dir(javadocJarFile.getName());
		getProject().copy((copy) -> copyJavadocPackageList(javadocJarFile, destination, copy));
	}

	private void copyJavadocPackageList(File javadocJarFile, Directory destination, CopySpec copy) {
		copy.from(getProject().zipTree(javadocJarFile).filter(this::isJavadocPackageListFile)).into(destination);
	}

	private boolean isJavadocPackageListFile(File file) {
		return JAVADOC_PACKAGE_LIST_FILES.contains(file.getName());
	}

	private List<String> links(ResolvedBom resolvedBom) {
		List<String> links = new ArrayList<>();
		links.add("https://docs.oracle.com/en/java/javase/17/docs/api/");
		links.add("https://jakarta.ee/specifications/platform/11/apidocs/");
		resolvedBom.libraries()
			.stream()
			.filter((candidate) -> INCLUDED_LIBRARIES.contains(candidate.name()))
			.flatMap((library) -> library.links().javadoc().stream())
			.map(JavadocLink::uri)
			.map(URI::toString)
			.forEach(links::add);
		return links;
	}

}
