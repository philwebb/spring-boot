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
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import org.springframework.boot.build.bom.ResolvedBom;
import org.springframework.util.StringUtils;

/**
 * Specialized {@link Javadoc} task for aggregated javadoc generation.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
public abstract class AggregatedJavadoc extends Javadoc {

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
		File extractDir = getProject().getLayout().getBuildDirectory().get().dir("docs/javadocpackagelist").getAsFile();
		extractPackageListFiles(extractDir);
		if (getProject().getGradle().getStartParameter().getLogLevel() == LogLevel.DEBUG) {
			options.addBooleanOption("offlinelinks-debug", true);
		}
		options.addStringOption("offlinelinks-source", new File(extractDir, "@name@").getAbsolutePath());
		resolvedBom.offlineJavadocLinks().forEach((url, jars) -> {
			String listOfJavadocJars = jars.stream()
				.filter((jar) -> Files.isDirectory(extractDir.toPath().resolve(jar)))
				.collect(Collectors.joining(","));
			if (StringUtils.hasLength(listOfJavadocJars)) {
				options.linksOffline(url.toString(), listOfJavadocJars);
			}
		});
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
