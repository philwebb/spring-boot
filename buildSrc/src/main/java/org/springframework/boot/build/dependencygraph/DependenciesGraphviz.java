/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.boot.build.dependencygraph;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.tasks.TaskAction;

/**
 * {@link Task} that generates a dependency graph file to use with Graphviz.
 *
 * @author Phillip Webb
 */
public abstract class DependenciesGraphviz extends DefaultTask {

	private static final Set<Pattern> IGNORED_PROJECTS;
	static {
		Set<String> ignored = new LinkedHashSet<>();
		ignored.add(".*-docs");
		ignored.add(".*-tests");
		ignored.add(":spring-boot-build");
		ignored.add(":spring-boot-project");
		ignored.add(":spring-boot-project:spring-boot-tools:.*");
		ignored.add(":spring-boot-project:spring-boot-starters:.*");
		ignored.add(":spring-boot-project:spring-boot");
		ignored.add(":spring-boot-project:spring-boot-autoconfigure");
		ignored.add(":spring-boot-project:spring-boot-actuator-autoconfigure");
		ignored.add(":spring-boot-project:spring-boot-actuator");
		ignored.add(":spring-boot-project:spring-boot-testcontainers");
		ignored.add(":spring-boot-project:spring-boot-dockercompose");
		ignored.add(":spring-boot-project:spring-boot-test-autoconfigure");
		ignored.add(":spring-boot-project:spring-boot-project");
		ignored.add(":spring-boot-project:spring-boot-tools");
		ignored.add(":spring-boot-project:spring-boot-metrics");
		ignored.add(":spring-boot-project:spring-boot-health");
		ignored.add(":spring-boot-project:spring-boot-dependencies");
		ignored.add(":spring-boot-project:spring-boot-parent");
		ignored.add(":spring-boot-project:spring-boot-starters");
		ignored.add(":spring-boot-system-tests");
		ignored.add(":spring-boot-tests");
		ignored.add(":spring-boot-tests:spring-boot-smoke-tests:.*");
		IGNORED_PROJECTS = Collections
			.unmodifiableSet(new LinkedHashSet<>(ignored.stream().map(Pattern::compile).toList()));
	}

	@TaskAction
	void createGraph() {
		Graph graph = new Graph();
		getProject().allprojects((project) -> {
			if (!isIgnored(project)) {
				graph.add(project);
			}
		});
		System.out.println(graph);
	}

	static boolean isIgnored(Project project) {
		return IGNORED_PROJECTS.stream().anyMatch((pattern) -> pattern.matcher(project.getPath()).matches());
	}

	static class Graph {

		ModuleSubgraph springBootModules = new ModuleSubgraph();

		ModuleSubgraph springModules = new ModuleSubgraph();

		ModuleSubgraph thirdPartyModules = new ModuleSubgraph();

		DependenciesSubgraph apiDependencies = new DependenciesSubgraph();

		DependenciesSubgraph optionalDependencies = new DependenciesSubgraph();

		void add(Project project) {
			ConfigurationContainer configurations = project.getConfigurations();
			this.springBootModules.addModule(project.getName());
			this.apiDependencies.addDependencies(project, configurations.findByName("api"));
			this.optionalDependencies.addDependencies(project, configurations.findByName("optional"));
		}

		@Override
		public String toString() {
			StringWriter writer = new StringWriter();
			PrintWriter out = new PrintWriter(writer);
			append(out);
			return writer.toString();
		}

		private void append(PrintWriter out) {
			out.println("digraph {");
			out.println("  graph [fontname=\"Helvetica\"; layout=neato; overlap=prism; "
					+ "overlap_scaling=-1.5; outputorder=nodesfirst; splines=curved;];");
			out.println("  node [shape=box; style=\"filled\"; penwidth=\"0.5\"; "
					+ "width=0; height=0; margin=\"0.05,0.05\"];");
			out.println("  edge [color=\"#000080\"; penwidth=\"0.5\"; arrowhead=\"open\"; arrowsize=\"0.7\"];");
			out.println();
			this.springBootModules.append(out, "spring-boot-modules", "fillcolor=olivedrab2;");
			this.springModules.append(out, "spring-modules", "fillcolor=lightcyan2;");
			this.thirdPartyModules.append(out, "third-party-modules", "fillcolor=lightcyan2");
			this.apiDependencies.append(out, "api", "penwidth=1.5");
			this.optionalDependencies.append(out, "optional", "style=dashed");
			out.println("}");
		}

		class ModuleSubgraph {

			private final Set<String> nodes = new TreeSet<>();

			void addModule(String name) {
				this.nodes.add(name);
			}

			void append(PrintWriter out, String name, String nodeStyle) {
				out.println("  subgraph \"" + name + "\" {");
				out.println("    node [" + nodeStyle + "];");
				this.nodes.forEach((node) -> out.println("    \"" + node + "\";"));
				out.println("  }");
				out.println();
			}

		}

		class DependenciesSubgraph {

			Map<String, Set<String>> dependencies = new TreeMap<>();

			void addDependencies(Project project, Configuration configuration) {
				Set<String> dependencies = this.dependencies.computeIfAbsent(project.getName(),
						(key) -> new TreeSet<>());
				if (configuration != null) {
					configuration.getAllDependencies().forEach((dependency) -> addDependency(dependencies, dependency));
				}
			}

			void append(PrintWriter out, String name, String edgeStyle) {
				out.println("  subgraph \"" + name + "\" {");
				out.println("    edge [" + edgeStyle + "];");
				this.dependencies.forEach((source, dests) -> appendEdge(out, source, dests));
				out.println("  }");
				out.println();
			}

			private void appendEdge(PrintWriter out, String source, Set<String> dests) {
				dests.forEach((dest) -> out.println("    \"" + source + "\" -> \"" + dest + "\";"));
			}

			private void addDependency(Set<String> dependencies, Dependency dependency) {
				if (dependency instanceof ProjectDependency projectDependency) {
					if (!isIgnored(projectDependency.getDependencyProject())) {
						dependencies.add(dependency.getName());
					}
				}
				else {
					if (true) {
						return;
					}
					String node = dependency.getGroup() + ":" + dependency.getName();
					dependencies.add(node);
					ModuleSubgraph moduleSubgraph = (dependency.getGroup().startsWith("org.springframework"))
							? Graph.this.springModules : Graph.this.thirdPartyModules;
					moduleSubgraph.addModule(node);
				}
			}

		}

	}

}
