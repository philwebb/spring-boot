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

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.UndertowHttpHandlerAdapter;
import org.springframework.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class UndertowReactiveWebServerFactory extends AbstractReactiveWebServerFactory
		implements ConfigurableUndertowWebServerFactory {

	private UndertowWebServerFactory factory = new UndertowWebServerFactory();

	/**
	 * Create a new {@link UndertowReactiveWebServerFactory} instance.
	 */
	public UndertowReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link UndertowReactiveWebServerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public void setBuilderCustomizers(Collection<? extends UndertowBuilderCustomizer> customizers) {
		this.factory.setBuilderCustomizers(customizers);
	}

	@Override
	public void addBuilderCustomizers(UndertowBuilderCustomizer... customizers) {
		this.factory.addBuilderCustomizers(customizers);
	}

	/**
	 * Returns a mutable collection of the {@link UndertowBuilderCustomizer}s that will be
	 * applied to the Undertow {@link io.undertow.Undertow.Builder Builder}.
	 * @return the customizers that will be applied
	 */
	public Collection<UndertowBuilderCustomizer> getBuilderCustomizers() {
		return this.factory.getBuilderCustomizers();
	}

	@Override
	public void setBufferSize(Integer bufferSize) {
		this.factory.setBufferSize(bufferSize);
	}

	@Override
	public void setIoThreads(Integer ioThreads) {
		this.factory.setIoThreads(ioThreads);
	}

	@Override
	public void setWorkerThreads(Integer workerThreads) {
		this.factory.setWorkerThreads(workerThreads);
	}

	@Override
	public void setUseDirectBuffers(Boolean directBuffers) {
		this.factory.setUseDirectBuffers(directBuffers);
	}

	@Override
	public void setUseForwardHeaders(boolean useForwardHeaders) {
		this.factory.setUseForwardHeaders(useForwardHeaders);
	}

	protected final boolean isUseForwardHeaders() {
		return this.factory.isUseForwardHeaders();
	}

	@Override
	public void setAccessLogDirectory(File accessLogDirectory) {
		this.factory.setAccessLogDirectory(accessLogDirectory);
	}

	@Override
	public void setAccessLogPattern(String accessLogPattern) {
		this.factory.setAccessLogPattern(accessLogPattern);
	}

	@Override
	public void setAccessLogPrefix(String accessLogPrefix) {
		this.factory.setAccessLogPrefix(accessLogPrefix);
	}

	@Override
	public void setAccessLogSuffix(String accessLogSuffix) {
		this.factory.setAccessLogSuffix(accessLogSuffix);
	}

	public boolean isAccessLogEnabled() {
		return this.factory.isAccessLogEnabled();
	}

	@Override
	public void setAccessLogEnabled(boolean accessLogEnabled) {
		this.factory.setAccessLogEnabled(accessLogEnabled);
	}

	@Override
	public void setAccessLogRotate(boolean accessLogRotate) {
		this.factory.setAccessLogRotate(accessLogRotate);
	}

	/////// FIXME

	// FIXME this is copy/paste so perhaps can be consolidated
	@Override
	public WebServer getWebServer(org.springframework.http.server.reactive.HttpHandler httpHandler) {
		Undertow.Builder builder = createBuilder(getPort());
		List<HttpHandlerFactory> factories = new ArrayList<>();
		factories.add((next) -> new UndertowHttpHandlerAdapter(httpHandler));
		Compression compression = getCompression();
		if (compression != null && compression.getEnabled()) {
			factories.add(new CompressionHttpHandlerFactory(compression));
		}
		if (this.useForwardHeaders) {
			factories.add(new ForwardHeadersHttpHandlerFactory());
		}
		if (StringUtils.hasText(getServerHeader())) {
			factories.add(new ServerHeaderHttpHandlerFactory(getServerHeader()));
		}
		Duration shutdownGracePeriod = getShutdown().getGracePeriod();
		if (shutdownGracePeriod != null) {
			factories.add(new GracefulShutdownHttpHandlerFactory(shutdownGracePeriod));
		}
		if (isAccessLogEnabled()) {
			factories.add(new AccessLogHttpHandlerFactory(this.accessLogDirectory, this.accessLogPattern,
					this.accessLogPrefix, this.accessLogSuffix, this.accessLogRotate));
		}
		return new UndertowWebServer(builder, factories, getPort() >= 0);
	}

	private Undertow.Builder createBuilder(int port) {
		Undertow.Builder builder = Undertow.builder();
		if (this.bufferSize != null) {
			builder.setBufferSize(this.bufferSize);
		}
		if (this.ioThreads != null) {
			builder.setIoThreads(this.ioThreads);
		}
		if (this.workerThreads != null) {
			builder.setWorkerThreads(this.workerThreads);
		}
		if (this.directBuffers != null) {
			builder.setDirectBuffers(this.directBuffers);
		}
		if (getSsl() != null && getSsl().isEnabled()) {
			customizeSsl(builder);
		}
		else {
			builder.addHttpListener(port, getListenAddress());
		}
		builder.setServerOption(UndertowOptions.SHUTDOWN_TIMEOUT, 0);
		for (UndertowBuilderCustomizer customizer : this.builderCustomizers) {
			customizer.customize(builder);
		}
		return builder;
	}

	private void customizeSsl(Undertow.Builder builder) {
		new SslBuilderCustomizer(getPort(), getAddress(), getSsl(), getSslStoreProvider()).customize(builder);
		if (getHttp2() != null) {
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, getHttp2().isEnabled());
		}
	}

	private String getListenAddress() {
		if (getAddress() == null) {
			return "0.0.0.0";
		}
		return getAddress().getHostAddress();
	}

}
