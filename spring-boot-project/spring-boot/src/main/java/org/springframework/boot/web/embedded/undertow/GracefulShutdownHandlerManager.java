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

import java.time.Duration;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;

/**
 * {@link HandlerManager} for graceful shutdown.
 *
 * @author Andy Wilkinson
 */
class GracefulShutdownHandlerManager implements HandlerManager {

	private final HandlerManager delegate;

	private final Duration shutdownGracePeriod;

	private volatile UndertowGracefulShutdown gracefulShutdown;

	GracefulShutdownHandlerManager(HandlerManager delegate, Duration shutdownGracePeriod) {
		this.delegate = delegate;
		this.shutdownGracePeriod = shutdownGracePeriod;
	}

	@Override
	public HttpHandler start() {
		GracefulShutdownHandler handler = new GracefulShutdownHandler(this.delegate.start());
		this.gracefulShutdown = new UndertowGracefulShutdown(handler, this.shutdownGracePeriod);
		return handler;
	}

	@Override
	public void stop() {
		this.delegate.stop();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T extract(Class<T> type) {
		if (type.equals(UndertowGracefulShutdown.class)) {
			return (T) this.gracefulShutdown;
		}
		return this.delegate.extract(type);
	}

}
