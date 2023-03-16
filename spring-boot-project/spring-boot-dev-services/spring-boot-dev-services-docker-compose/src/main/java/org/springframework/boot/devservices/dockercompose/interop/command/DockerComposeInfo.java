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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.util.List;
import java.util.Set;

/**
 * Information about docker compose.
 *
 * @param executable executable to use
 * @param version docker compose version
 * @param activeProfiles active profiles
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record DockerComposeInfo(List<String> executable, String version, Set<String> activeProfiles) {
	@Override
	public String toString() {
		String executableAsString = String.join(" ", this.executable);
		if (this.activeProfiles.isEmpty()) {
			return "executable '%s' version %s".formatted(executableAsString, this.version);
		}
		else {
			return "executable '%s' version %s, active profiles: %s".formatted(executableAsString, this.version,
					String.join(", ", this.activeProfiles));
		}
	}
}
