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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.sound.sampled.Port;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.web.server.GracefulShutdown;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 */
abstract class AbstractUndertowWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(AbstractUndertowWebServer.class);

	// FIXME make private
	protected final Object monitor = new Object();

	// FIXME make private
	protected volatile boolean started = false;

	// FIXME make private
	protected Undertow undertow;

	// FIXME make private final
	protected boolean autoStart;

	@Override
	public void start() throws WebServerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			try {
				if (!this.autoStart) {
					return;
				}
				if (this.undertow == null) {
					this.undertow = createUndertowServer();
				}
				this.undertow.start();
				this.started = true;
				logger.info("Undertow started on port(s) " + getPortsDescription());
				// FIXME
				// logger.info("Undertow started on port(s) " + getPortsDescription()
				// + " with context path '" + this.contextPath + "'");
			}
			catch (Exception ex) {
				try {
					PortInUseException.ifPortBindingException(ex, (bindException) -> {
						List<Port> failedPorts = getConfiguredPorts();
						failedPorts.removeAll(getActualPorts());
						if (failedPorts.size() == 1) {
							throw new PortInUseException(failedPorts.get(0).getNumber());
						}
					});
					throw new WebServerException("Unable to start embedded Undertow", ex);
				}
				finally {
					stopSilently();
				}
			}
		}
	}

	private Undertow createUndertowServer() throws ServletException {
		HttpHandler handler = this.manager.start();
		this.closeables = new ArrayList<>();
		this.gracefulShutdown = null;
		for (HttpHandlerFactory factory : this.httpHandlerFactories) {
			handler = factory.getHandler(handler);
			if (handler instanceof Closeable) {
				this.closeables.add((Closeable) handler);
			}
			if (handler instanceof GracefulShutdown) {
				Assert.isNull(this.gracefulShutdown, "Only a single GracefulShutdown handler can be defined");
				this.gracefulShutdown = (GracefulShutdown) handler;
			}
		}
		if (!StringUtils.isEmpty(getContextPath())) {
			handler = Handlers.path().addPrefixPath(getContextPath(), handler);
		}
		this.builder.setHandler(handler);
		return this.builder.build();
	}

	protected abstract String getContextPath();

}
