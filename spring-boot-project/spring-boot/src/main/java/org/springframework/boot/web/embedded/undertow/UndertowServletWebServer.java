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

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.channels.BoundChannel;

import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link WebServer} that can be used to control an embedded Undertow server. Typically
 * this class should be created using {@link UndertowServletWebServerFactory} and not
 * directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @author Kristine Jetzke
 * @since 2.0.0
 * @see UndertowServletWebServerFactory
 */
public class UndertowServletWebServer implements WebServer {

	private static final Log logger = LogFactory.getLog(UndertowServletWebServer.class);

	private final Object monitor = new Object();

	private final Builder builder;

	private final HandlerManager handlerManager;

	private final String contextPath;

	private final boolean autoStart;

	private Undertow undertow;

	private volatile boolean started = false;

	/**
	 * Create a new {@link UndertowServletWebServer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 */
	public UndertowServletWebServer(Builder builder, DeploymentManager manager, String contextPath, boolean autoStart,
			Compression compression) {
		this(builder, manager, contextPath, false, autoStart, compression);
	}

	/**
	 * Create a new {@link UndertowServletWebServer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param useForwardHeaders if x-forward headers should be used
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 */
	public UndertowServletWebServer(Builder builder, DeploymentManager manager, String contextPath,
			boolean useForwardHeaders, boolean autoStart, Compression compression) {
		this(builder, manager, contextPath, useForwardHeaders, autoStart, compression, null);
	}

	/**
	 * Create a new {@link UndertowServletWebServer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param useForwardHeaders if x-forward headers should be used
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 * @param serverHeader string to be used in HTTP header
	 */
	public UndertowServletWebServer(Builder builder, DeploymentManager manager, String contextPath,
			boolean useForwardHeaders, boolean autoStart, Compression compression, String serverHeader) {
		this(builder, createHandlerManager(manager, useForwardHeaders, compression, serverHeader, null), contextPath,
				autoStart);
	}

	/**
	 * Create a new {@link UndertowServletWebServer} instance.
	 * @param builder the builder
	 * @param handlerManager the manager for the {@link HttpHandler}
	 * @param contextPath the root context path
	 * @param autoStart if the server should be started
	 * @since 2.3.0
	 */
	public UndertowServletWebServer(Builder builder, HandlerManager handlerManager, String contextPath,
			boolean autoStart) {
		this.builder = builder;
		this.handlerManager = handlerManager;
		this.contextPath = contextPath;
		this.autoStart = autoStart;
	}

	private static HandlerManager createHandlerManager(DeploymentManager deploymentManager, boolean useForwardHeaders,
			Compression compression, String serverHeader, Duration shutdownGracePeriod) {
		HandlerManager handlerManager = new DeploymentManagerHandlerManager(deploymentManager);
		if (compression != null && compression.getEnabled()) {
			handlerManager = new CompressionHandlerManager(handlerManager, compression);
		}
		if (useForwardHeaders) {
			handlerManager = new ForwardHeadersHandlerManager(handlerManager);
		}
		if (StringUtils.hasText(serverHeader)) {
			handlerManager = new ServerHeaderHandlerManager(handlerManager, serverHeader);
		}
		if (shutdownGracePeriod != null) {
			handlerManager = new GracefulShutdownHandlerManager(handlerManager, shutdownGracePeriod);
		}

		return handlerManager;
	}

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
				UndertowServletWebServer.logger.info("Undertow started on port(s) " + getPortsDescription()
						+ " with context path '" + this.contextPath + "'");
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

	public DeploymentManager getDeploymentManager() {
		synchronized (this.monitor) {
			return this.handlerManager.extract(DeploymentManager.class);
		}
	}

	private void stopSilently() {
		try {
			if (this.undertow != null) {
				this.undertow.stop();
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	private Undertow createUndertowServer() {
		HttpHandler httpHandler = this.handlerManager.start();
		httpHandler = getContextHandler(httpHandler);
		this.builder.setHandler(httpHandler);
		return this.builder.build();
	}

	private HttpHandler getContextHandler(HttpHandler httpHandler) {
		HttpHandler contextHandler = httpHandler;
		if (StringUtils.isEmpty(this.contextPath)) {
			return contextHandler;
		}
		return Handlers.path().addPrefixPath(this.contextPath, contextHandler);
	}

	private String getPortsDescription() {
		List<Port> ports = getActualPorts();
		if (!ports.isEmpty()) {
			return StringUtils.collectionToDelimitedString(ports, " ");
		}
		return "unknown";
	}

	private List<Port> getActualPorts() {
		List<Port> ports = new ArrayList<>();
		try {
			if (!this.autoStart) {
				ports.add(new Port(-1, "unknown"));
			}
			else {
				for (BoundChannel channel : extractChannels()) {
					ports.add(getPortFromChannel(channel));
				}
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<BoundChannel> extractChannels() {
		Field channelsField = ReflectionUtils.findField(Undertow.class, "channels");
		ReflectionUtils.makeAccessible(channelsField);
		return (List<BoundChannel>) ReflectionUtils.getField(channelsField, this.undertow);
	}

	private Port getPortFromChannel(BoundChannel channel) {
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress) {
			String protocol = (ReflectionUtils.findField(channel.getClass(), "ssl") != null) ? "https" : "http";
			return new Port(((InetSocketAddress) socketAddress).getPort(), protocol);
		}
		return null;
	}

	private List<Port> getConfiguredPorts() {
		List<Port> ports = new ArrayList<>();
		for (Object listener : extractListeners()) {
			try {
				Port port = getPortFromListener(listener);
				if (port.getNumber() != 0) {
					ports.add(port);
				}
			}
			catch (Exception ex) {
				// Continue
			}
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<Object> extractListeners() {
		Field listenersField = ReflectionUtils.findField(Undertow.class, "listeners");
		ReflectionUtils.makeAccessible(listenersField);
		return (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
	}

	private Port getPortFromListener(Object listener) {
		Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
		ReflectionUtils.makeAccessible(typeField);
		String protocol = ReflectionUtils.getField(typeField, listener).toString();
		Field portField = ReflectionUtils.findField(listener.getClass(), "port");
		ReflectionUtils.makeAccessible(portField);
		int port = (Integer) ReflectionUtils.getField(portField, listener);
		return new Port(port, protocol);
	}

	@Override
	public void stop() throws WebServerException {
		synchronized (this.monitor) {
			if (!this.started) {
				return;
			}
			this.started = false;
			try {
				this.handlerManager.stop();
				this.undertow.stop();
			}
			catch (Exception ex) {
				throw new WebServerException("Unable to stop undertow", ex);
			}
		}
	}

	@Override
	public int getPort() {
		List<Port> ports = getActualPorts();
		if (ports.isEmpty()) {
			return 0;
		}
		return ports.get(0).getNumber();
	}

	@Override
	public boolean shutDownGracefully() {
		UndertowGracefulShutdown gracefulShutdown = this.handlerManager.extract(UndertowGracefulShutdown.class);
		return (gracefulShutdown != null) ? gracefulShutdown.shutDownGracefully() : false;
	}

	boolean inGracefulShutdown() {
		UndertowGracefulShutdown gracefulShutdown = this.handlerManager.extract(UndertowGracefulShutdown.class);
		return (gracefulShutdown != null) ? gracefulShutdown.isShuttingDown() : false;
	}

	/**
	 * An active Undertow port.
	 */
	private static final class Port {

		private final int number;

		private final String protocol;

		private Port(int number, String protocol) {
			this.number = number;
			this.protocol = protocol;
		}

		int getNumber() {
			return this.number;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Port other = (Port) obj;
			return this.number == other.number;
		}

		@Override
		public int hashCode() {
			return this.number;
		}

		@Override
		public String toString() {
			return this.number + " (" + this.protocol + ")";
		}

	}

}
