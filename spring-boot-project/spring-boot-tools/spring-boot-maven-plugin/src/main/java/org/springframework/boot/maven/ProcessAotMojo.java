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
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.toolchain.ToolchainManager;

import org.springframework.util.ObjectUtils;

/**
 * Invoke the AOT engine on the application.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 3.0.0
 */
@Mojo(name = "process-aot", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true,
		requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
		requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ProcessAotMojo extends AbstractAotMojo {

	private static final String AOT_PROCESSOR_CLASS_NAME = "org.springframework.boot.AotProcessor";

	/**
	 * The current Maven session. This is used for toolchain manager API calls.
	 */
	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	/**
	 * The toolchain manager to use to locate a custom JDK.
	 */
	@Component
	private ToolchainManager toolchainManager;

	/**
	 * Directory containing the classes and resource files that should be packaged into
	 * the archive.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File classesDirectory;

	/**
	 * Directory containing the generated sources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/sources", required = true)
	private File generatedSources;

	/**
	 * Directory containing the generated resources.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/resources", required = true)
	private File generatedResources;

	/**
	 * Directory containing the generated classes.
	 */
	@Parameter(defaultValue = "${project.build.directory}/spring-aot/main/classes", required = true)
	private File generatedClasses;

	/**
	 * List of JVM system properties to pass to the AOT process.
	 */
	@Parameter
	private Map<String, String> systemPropertyVariables;

	/**
	 * JVM arguments that should be associated with the AOT process. On command line, make
	 * sure to wrap multiple values between quotes.
	 */
	@Parameter(property = "spring-boot.aot.jvmArguments")
	private String jvmArguments;

	/**
	 * Name of the main class to use as the source for the AOT process. If not specified
	 * the first compiled class found that contains a 'main' method will be used.
	 */
	@Parameter(property = "spring-boot.aot.main-class")
	private String mainClass;

	/**
	 * Spring profiles to take into account for AOT processing.
	 */
	@Parameter
	private String[] profiles;

	@Override
	protected void executeAot() throws Exception {
		generateAotAssets();
		compileSourceFiles(this.generatedSources, this.classesDirectory);
		copyAll(this.generatedResources.toPath().resolve("META-INF/native-image"),
				this.classesDirectory.toPath().resolve("META-INF/native-image"));
		copyAll(this.generatedClasses.toPath(), this.classesDirectory.toPath());
	}

	private void generateAotAssets() throws Exception {
		String applicationClass = (this.mainClass != null) ? this.mainClass
				: SpringBootApplicationClassFinder.findSingleClass(this.classesDirectory);
		List<String> command = CommandLineBuilder.forMainClass(AOT_PROCESSOR_CLASS_NAME)
				.withSystemProperties(this.systemPropertyVariables)
				.withJvmArguments(new RunArguments(this.jvmArguments).asArray()).withClasspath(getClassPath())
				.withArguments(getAotArguments(applicationClass)).build();
		if (getLog().isDebugEnabled()) {
			getLog().debug("Generating AOT assets using command: " + command);
		}
		JavaProcessExecutor processExecutor = new JavaProcessExecutor(this.session, this.toolchainManager);
		processExecutor.run(this.project.getBasedir(), command, Collections.emptyMap());
	}

	@Override
	protected URL[] getClassPath() throws Exception {
		return getClassPath(this.classesDirectory, new TestScopeArtifactFilter());
	}

	private String[] getAotArguments(String applicationClass) {
		List<String> aotArguments = new ArrayList<>();
		aotArguments.add(applicationClass);
		aotArguments.add(this.generatedSources.toString());
		aotArguments.add(this.generatedResources.toString());
		aotArguments.add(this.generatedClasses.toString());
		aotArguments.add(this.project.getGroupId());
		aotArguments.add(this.project.getArtifactId());
		if (!ObjectUtils.isEmpty(this.profiles)) {
			aotArguments.add("--spring.profiles.active=" + String.join(",", this.profiles));
		}
		return aotArguments.toArray(String[]::new);
	}

	private void copyAll(Path from, Path to) throws IOException {
		List<Path> files = (Files.exists(from)) ? Files.walk(from).filter(Files::isRegularFile).toList()
				: Collections.emptyList();
		for (Path file : files) {
			String relativeFileName = file.subpath(from.getNameCount(), file.getNameCount()).toString();
			getLog().debug("Copying '" + relativeFileName + "' to " + to);
			Path target = to.resolve(relativeFileName);
			Files.createDirectories(target.getParent());
			Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
