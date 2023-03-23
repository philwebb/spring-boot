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

import java.util.Map;

/**
 * @author pwebb
 */
public interface RunningService extends DefinedService {

	// getOrigin from ComposeFile and Service Name


	String host(); // A bunch of logic

	Map<Integer, Port> ports(); // from inspect output with a bunch of logic

	Port getMappedPort(int sourcePort); // just on ports. Rich type

	Map<String, String> env(); // from inspect env (processed)

	Map<String, String> labels(); // from inspect

	String logicalTypeName(); // This is the type, not the name. Probably don't want it
	// here

	boolean ignore(); // FIXME do we want this here?

}
