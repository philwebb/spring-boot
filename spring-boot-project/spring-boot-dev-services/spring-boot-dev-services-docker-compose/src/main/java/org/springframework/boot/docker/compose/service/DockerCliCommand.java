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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Commands that can be executed by the {@link DockerCli}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract sealed class DockerCliCommand<R> {

	private final Type type;

	private final Class<?> responseType;

	private final boolean listResponse;

	private final List<String> command;

	private DockerCliCommand(Type type, Class<?> responseType, boolean listResponse, String... command) {
		this.type = type;
		this.responseType = responseType;
		this.listResponse = listResponse;
		this.command = List.of(command);
	}

	Type getType() {
		return this.type;
	}

	List<String> getCommand() {
		return this.command;
	}

	@SuppressWarnings("unchecked")
	R deserialize(String json) {
		if (this.responseType == Void.class) {
			return null;
		}
		return (R) ((!this.listResponse) ? DockerJson.deserialize(json, this.responseType)
				: DockerJson.deserializeToList(json, this.responseType));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.responseType, this.listResponse, this.command);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DockerCliCommand<?> other = (DockerCliCommand<?>) obj;
		boolean result = true;
		result = result && this.type == other.type;
		result = result && this.responseType == other.responseType;
		result = result && this.listResponse == other.listResponse;
		result = result && this.command.equals(other.command);
		return result;
	}

	@Override
	public String toString() {
		return "DockerCliCommand [type=%s, responseType=%s, listResponse=%s, command=%s]".formatted(this.type,
				this.responseType, this.listResponse, this.command);
	}

	protected static String[] join(Collection<String> command, Collection<String> args) {
		List<String> result = new ArrayList<>(command);
		result.addAll(args);
		return result.toArray(new String[0]);
	}

	/**
	 * The {@code docker context} command.
	 */
	final static class Context extends DockerCliCommand<List<DockerCliContextResponse>> {

		Context() {
			super(Type.DOCKER, DockerCliContextResponse.class, true, "context", "ls", "--format={{ json . }}");
		}

	}

	/**
	 * The {@code docker inspect} command.
	 */
	final static class Inspect extends DockerCliCommand<List<DockerCliInspectResponse>> {

		Inspect(Collection<String> ids) {
			super(Type.DOCKER, DockerCliInspectResponse.class, true,
					join(List.of("inspect", "--format={{ json . }}"), ids));
		}

	}

	/**
	 * The {@code docker compose config} command.
	 */
	final static class ComposeConfig extends DockerCliCommand<DockerCliComposeConfigResponse> {

		ComposeConfig() {
			super(Type.DOCKER_COMPOSE, DockerCliComposeConfigResponse.class, false, "config", "--format=json");
		}

	}

	/**
	 * The {@code docker compose ps} command.
	 */
	final static class ComposePs extends DockerCliCommand<List<DockerCliComposePsResponse>> {

		ComposePs() {
			super(Type.DOCKER_COMPOSE, DockerCliComposePsResponse.class, true, "ps", "--format=json");
		}

	}

	/**
	 * The {@code docker compose up} command.
	 */
	final static class ComposeUp extends DockerCliCommand<Void> {

		ComposeUp() {
			super(Type.DOCKER_COMPOSE, Void.class, false, "up", "--no-color", "--quiet-pull", "--detach", "--wait");
		}

	}

	/**
	 * The {@code docker compose down} command.
	 */
	final static class ComposeDown extends DockerCliCommand<Void> {

		ComposeDown(Duration timeout) {
			super(Type.DOCKER_COMPOSE, Void.class, false, "down", "--timeout", Long.toString(timeout.toSeconds()));
		}

	}

	/**
	 * The {@code docker compose start} command.
	 */
	final static class ComposeStart extends DockerCliCommand<Void> {

		ComposeStart() {
			super(Type.DOCKER_COMPOSE, Void.class, false, "start", "--no-color", "--quiet-pull", "--detach", "--wait");
		}

	}

	/**
	 * The {@code docker compose stop} command.
	 */
	final static class ComposeStop extends DockerCliCommand<Void> {

		ComposeStop(Duration timeout) {
			super(Type.DOCKER_COMPOSE, Void.class, false, "stop", "--timeout", Long.toString(timeout.toSeconds()));
		}

	}

	/**
	 * Command Types.
	 */
	enum Type {

		/**
		 * A command executed using {@code docker}.
		 */
		DOCKER,

		/**
		 * A command executed using {@code docker compose} or {@code docker-compose}.
		 */
		DOCKER_COMPOSE

	}

}
