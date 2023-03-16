/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devservices.dockercompose.interop;

/**
 * A docker image name.
 *
 * @param tag tag or {@code null}
 * @param project project or {@code null}
 * @param image image
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
// See https://docs.docker.com/compose/compose-file/#image
// [<registry>/][<project>/]<image>[:<tag>|@<digest>]
public record DockerImageName(String project, String image, String tag) {

	/**
	 * Parses a docker image name from a string.
	 * @param value the string to parse
	 * @return the parsed docker image name
	 */
	public static DockerImageName parse(String value) {
		String input = value;
		// Strip digest
		int digestStart = input.lastIndexOf('@');
		if (digestStart != -1) {
			input = input.substring(0, digestStart);
		}
		// Parse tag
		int lastSlash = input.lastIndexOf('/');
		int tagStart = input.lastIndexOf(':');
		boolean portNumberInRegistry = lastSlash > tagStart;
		String tag = null;
		if (tagStart != -1 && !portNumberInRegistry) {
			tag = input.substring(tagStart + 1);
			input = input.substring(0, tagStart);
		}
		// Parse image
		int imageStart = input.lastIndexOf('/');
		String image;
		if (imageStart == -1) {
			image = input;
			input = "";
		}
		else {
			image = input.substring(imageStart + 1);
			input = input.substring(0, imageStart);
		}
		// Parse project
		String project = null;
		if (!input.isEmpty()) {
			int projectStart = input.lastIndexOf('/');
			if (projectStart == -1) {
				project = input;
			}
			else {
				project = input.substring(projectStart + 1);
			}
		}
		// Project and registry is sometimes ambiguous
		if (project != null && (project.contains(".") || project.contains(":"))) {
			project = null;
		}
		return new DockerImageName(project, image, tag);
	}
}
