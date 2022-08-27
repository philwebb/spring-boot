/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.maven;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Invoke the AOT engine on tests.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@Mojo(name = "process-test-aot", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class ProcessTestAotMojo extends AbstractAotMojo {

	private static final String AOT_PROCESSOR_CLASS_NAME = "org.springframework.test.context.aot.TestAotProcessor";

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Directory containing the generated sources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/sources", required = true)
	private File generatedSources;

	/**
	 * Directory containing the generated resources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/resources", required = true)
	private File generatedResources;

	/**
	 * Directory containing the generated classes.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/test/classes", required = true)
	private File generatedClasses;

	@Override
	protected void executeAot() throws Exception {
		if (Boolean.getBoolean("skipTests") || Boolean.getBoolean("maven.test.skip")) {
			getLog().info("Skipping AOT test processing since tests are skipped");
			return;
		}
		Path testOutputDirectory = Paths.get(this.project.getBuild().getTestOutputDirectory());
		if (Files.notExists(testOutputDirectory)) {
			getLog().info("Skipping AOT test processing since no tests have been detected");
			return;
		}
		generateAotAssets(AOT_PROCESSOR_CLASS_NAME, getAotArguments());
		compileSourceFiles(this.generatedSources, this.classesDirectory);
	}

	private String[] getAotArguments() {
		List<String> aotArguments = new ArrayList<>();
		aotArguments.add(this.classesDirectory.toPath().toAbsolutePath().normalize().toString());
		aotArguments.add(this.generatedSources.toString());
		aotArguments.add(this.generatedResources.toString());
		aotArguments.add(this.generatedClasses.toString());
		aotArguments.add(this.project.getGroupId());
		aotArguments.add(this.project.getArtifactId());
		return aotArguments.toArray(String[]::new);
	}

	@Override
	protected URL[] getClassPath() throws Exception {
		return getClassPath(this.classesDirectory);
	}

}
