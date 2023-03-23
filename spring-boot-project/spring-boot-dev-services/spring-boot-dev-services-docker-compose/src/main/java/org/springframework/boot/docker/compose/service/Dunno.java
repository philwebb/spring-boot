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

package org.springframework.boot.docker.compose.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author pwebb
 */
public class Dunno {

	public void dunno() {
		DockerCli cli = null;
		List<DockerComposeProcessStatusResponse> processStatuses = null;
		Map<String, DockerInspectResponse> inspected = inspectRunning(cli, processStatuses);
		for (DockerComposeProcessStatusResponse processStatus : processStatuses) {
			DockerInspectResponse inspect = inspected.get(processStatus.id());
		}

		System.out.println(inspected);
	}

	private Map<String, DockerInspectResponse> inspectRunning(DockerCli cli,
			List<DockerComposeProcessStatusResponse> processStatuses) {
		List<String> running = processStatuses.stream()
			.filter(this::isRunning)
			.map(DockerComposeProcessStatusResponse::id)
			.toList();
		return cli.run(new DockerCommand.Inspect(running))
			.stream()
			.collect(Collectors.toMap(DockerInspectResponse::id, Function.identity()));
	}

	private boolean isRunning(DockerComposeProcessStatusResponse ps) {
		return !"exited".equals(ps.state());
	}

}
