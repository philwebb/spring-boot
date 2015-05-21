/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.developertools.restart.server;

import java.io.IOException;

import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.boot.developertools.restart.classloader.ClassLoaderFiles;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A HTTP server that can be used to upload updated {@link ClassLoaderFiles} and trigger
 * restarts.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpRestartServer {

	private ClassLoaderFiles a;

	private Restarter r;

	public void handle(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException {

	}

}
