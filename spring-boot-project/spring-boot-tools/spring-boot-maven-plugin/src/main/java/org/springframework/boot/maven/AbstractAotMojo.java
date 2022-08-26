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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import org.springframework.boot.maven.CommandLineBuilder.ClasspathBuilder;

/**
 * Abstract base class for AOT processing MOJOs.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
public abstract class AbstractAotMojo extends AbstractDependencyFilterMojo {

	/**
	 * Skip the execution.
	 */
	@Parameter(property = "spring-boot.aot.skip", defaultValue = "false")
	private boolean skip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (this.skip) {
			getLog().debug("Skipping AOT execution as per configuration");
			return;
		}
		try {
			executeAot();
		}
		catch (Exception ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
	}

	protected abstract void executeAot() throws Exception;

	protected final void compileSourceFiles(File sourcesDirectory, File outputDirectory) throws Exception {
		List<Path> sourceFiles = Files.walk(sourcesDirectory.toPath()).filter(Files::isRegularFile).toList();
		if (sourceFiles.isEmpty()) {
			return;
		}
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
			List<String> options = new ArrayList<>();
			options.add("-cp");
			options.add(ClasspathBuilder.build(Arrays.asList(getClassPath())));
			options.add("-d");
			options.add(outputDirectory.toPath().toAbsolutePath().toString());
			Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles);
			Errors errors = new Errors();
			CompilationTask task = compiler.getTask(null, fileManager, errors, options, null, compilationUnits);
			boolean result = task.call();
			if (!result || errors.hasReportedErrors()) {
				throw new IllegalStateException("Unable to compile generated source" + errors);
			}
		}
	}

	protected abstract URL[] getClassPath() throws Exception;

	protected final URL[] getClassPath(File classesDirectory, ArtifactsFilter... artifactFilters)
			throws MojoExecutionException {
		List<URL> urls = new ArrayList<>();
		urls.add(toURL(classesDirectory));
		urls.addAll(getDependencyURLs(artifactFilters));
		return urls.toArray(URL[]::new);
	}

	/**
	 * {@link DiagnosticListener} used to collect errors.
	 */
	protected static class Errors implements DiagnosticListener<JavaFileObject> {

		private final StringBuilder message = new StringBuilder();

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
			if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
				this.message.append("\n");
				this.message.append(diagnostic.getMessage(Locale.getDefault()));
				this.message.append(" ");
				this.message.append(diagnostic.getSource().getName());
				this.message.append(" ");
				this.message.append(diagnostic.getLineNumber()).append(":").append(diagnostic.getColumnNumber());
			}
		}

		boolean hasReportedErrors() {
			return this.message.length() > 0;
		}

		@Override
		public String toString() {
			return this.message.toString();
		}

	}

}
