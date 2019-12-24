/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

/**
 * The {@code 'dockerfile'} tools command.
 *
 * @author Phillip Webb
 */
class DockerfileCommand extends Command {

	static final Option FOLDER_OPTION = Option.of("folder", "string", "The folder contianing the jar");

	static final Option NO_WILDCARDS_OPTION = Option.flag("no-wildcards", "Use the full jar name rather than '*.jar'");

	private static final String DEFAULT_FROM = "adoptopenjdk:11-jre-hotspot";

	private static final String DEFAULT_LAUNCHER = "org.springframework.boot.loader.JarLauncher";

	private final Context context;

	DockerfileCommand(Context context) {
		super("dockerfile", "Generates a Dockerfile for the jar", Options.of(FOLDER_OPTION, NO_WILDCARDS_OPTION),
				Parameters.of("[<from>]"));
		this.context = context;
	}

	@Override
	protected void run(Map<Option, String> options, List<String> parameters) {
		run(System.out, options, parameters);
	}

	void run(PrintStream out, Map<Option, String> options, List<String> parameters) {
		String from = (parameters.size() > 0) ? parameters.get(0) : DEFAULT_FROM;
		outputBuilderSection(out, options, from);
		outputMainSection(out, from);
	}

	private void outputBuilderSection(PrintStream out, Map<Option, String> options, String from) {
		String folder = (options.getOrDefault(FOLDER_OPTION, this.context.getRelativeJarDir()));
		folder = (folder != null) ? folder : "";
		boolean noWildcards = options.containsKey(NO_WILDCARDS_OPTION);
		String jar = noWildcards ? this.context.getJarFile().getName() : "*.jar";
		out.println("FROM " + from + " AS builder");
		out.println("WORKDIR application");
		out.println("ARG JAR=" + slashAppend(folder, jar));
		out.println("COPY ${JAR} application.jar");
		out.println("RUN java -Djarmode=tools -jar application.jar extract-layers");
		out.println();
	}

	private void outputMainSection(PrintStream out, String from) {
		Layers layers = Layers.get(this.context);
		out.println("FROM " + from);
		out.println("WORKDIR application");
		for (String layer : layers) {
			out.println("COPY --from=builder application/" + layer + "/ ./");
		}
		out.println("ENTRYPOINT [\"java\", \"" + getLauncher() + "\"]");
	}

	private String getLauncher() {
		String launcher = findLauncher();
		return (launcher != null) ? launcher : DEFAULT_LAUNCHER;
	}

	private String findLauncher() {
		try {
			ClassLoader classLoader = getClass().getClassLoader().getParent();
			Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				URL resource = resources.nextElement();
				Manifest manifest = getManifest(resource);
				if (manifest != null) {
					String startClass = manifest.getMainAttributes().getValue("Start-Class");
					String mainClass = manifest.getMainAttributes().getValue("Main-Class");
					if (startClass != null && mainClass != null) {
						return mainClass;
					}
				}
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	private Manifest getManifest(URL resource) {
		try {
			URLConnection connection = resource.openConnection();
			if (connection instanceof JarURLConnection) {
				return ((JarURLConnection) connection).getManifest();
			}
			try (InputStream stream = connection.getInputStream()) {
				return new Manifest(stream);
			}
		}
		catch (IOException ex) {
			return null;
		}
	}

	private String slashAppend(String prefix, String postfix) {
		prefix = (prefix.length() == 0 || prefix.endsWith("/")) ? prefix : prefix + "/";
		return prefix + postfix;
	}

}
