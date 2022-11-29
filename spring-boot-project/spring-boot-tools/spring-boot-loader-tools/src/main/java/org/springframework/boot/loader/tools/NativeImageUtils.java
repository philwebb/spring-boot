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

package org.springframework.boot.loader.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilities for working with GraalVM native images.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
public final class NativeImageUtils {

	private NativeImageUtils() {
	}

	/**
	 * Creates the arguments passed to native-image for the exclusion of reachability
	 * metadata.
	 * @param excludes dependencies for which the reachability metadata should be excluded
	 * @return arguments for native-image
	 */
	public static List<String> createExcludeConfigArguments(Iterable<String> excludes) {
		List<String> args = new ArrayList<>();
		for (String exclude : excludes) {
			int lastSlash = exclude.lastIndexOf('/');
			String jar = (lastSlash != -1) ? exclude.substring(lastSlash + 1) : exclude;
			args.add("--exclude-config");
			args.add(Pattern.quote(jar));
			args.add("^/META-INF/native-image/.*");
		}
		return args;
	}

}
