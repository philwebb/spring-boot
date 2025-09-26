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

package org.springframework.boot.build.dependencies;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ExpectedDependenciesPlugin}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ExpectedDependenciesPluginIntegrationTests {

	private File projectDir;

	private File buildFile;

	@BeforeEach
	void setup(@TempDir File projectDir) {
		this.projectDir = projectDir;
		this.buildFile = new File(this.projectDir, "build.gradle");
	}

	@Test
	void expectedConfigurationIsCreated() throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins { id 'org.springframework.boot.expected-dependencies' }");
			out.println("task printConfigurations {");
			out.println("    doLast {");
			out.println("        configurations.all { println it.name }");
			out.println("    }");
			out.println("}");
		}
		BuildResult buildResult = runGradle("printConfigurations");
		assertThat(buildResult.getOutput()).contains(ExpectedDependenciesPlugin.EXPECTED_CONFIGURATION_NAME);
	}

	@Test
	void expectedDependenciesAreAddedToMainSourceSetsCompileClasspath() throws IOException {
		expectedDependenciesAreAddedToSourceSetClasspath("main", "compileClasspath");
	}

	@Test
	void expectedDependenciesAreAddedToMainSourceSetsRuntimeClasspath() throws IOException {
		expectedDependenciesAreAddedToSourceSetClasspath("main", "runtimeClasspath");
	}

	@Test
	void expectedDependenciesAreAddedToTestSourceSetsCompileClasspath() throws IOException {
		expectedDependenciesAreAddedToSourceSetClasspath("test", "compileClasspath");
	}

	@Test
	void expectedDependenciesAreAddedToTestSourceSetsRuntimeClasspath() throws IOException {
		expectedDependenciesAreAddedToSourceSetClasspath("test", "runtimeClasspath");
	}

	private void expectedDependenciesAreAddedToSourceSetClasspath(String sourceSet, String classpath)
			throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.expected-dependencies'");
			out.println("    id 'java'");
			out.println("}");
			out.println("repositories {");
			out.println("    mavenCentral()");
			out.println("}");
			out.println("dependencies {");
			out.println("    expected 'org.springframework:spring-jcl:5.1.2.RELEASE'");
			out.println("}");
			out.println("task printClasspath {");
			out.println("    doLast {");
			out.println("        println sourceSets." + sourceSet + "." + classpath + ".files");
			out.println("    }");
			out.println("}");
		}
		BuildResult buildResult = runGradle("printClasspath");
		assertThat(buildResult.getOutput()).contains("spring-jcl");
	}

	private BuildResult runGradle(String... args) {
		return GradleRunner.create().withProjectDir(this.projectDir).withArguments(args).withPluginClasspath().build();
	}

}
