/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.SetHeaderHandler;

/**
 * {@link HandlerManager} for a {@link SetHeaderHandler} that sets the {@code Server}
 * header.
 *
 * @author Andy Wilkinson
 */
class ServerHeaderHandlerManager implements HandlerManager {

	private final HandlerManager delegate;

	private final String serverHeader;

	ServerHeaderHandlerManager(HandlerManager delegate, String serverHeader) {
		this.delegate = delegate;
		this.serverHeader = serverHeader;
	}

	@Override
	public HttpHandler start() {
		return Handlers.header(this.delegate.start(), "Server", this.serverHeader);
	}

	@Override
	public void stop() {
		this.delegate.stop();
	}

	@Override
	public <T> T extract(Class<T> type) {
		return this.delegate.extract(type);
	}

}
