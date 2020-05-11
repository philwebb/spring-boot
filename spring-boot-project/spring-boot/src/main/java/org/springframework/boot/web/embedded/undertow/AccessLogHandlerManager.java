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
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import org.springframework.util.Assert;

/**
 * A {@link HandlerManager} for an {@link AccessLogHandler}.
 *
 * @author Andy Wilkinson
 */
class AccessLogHandlerManager implements HandlerManager {

	private final HandlerManager delegate;

	private final File accessLogDirectory;

	private final String accessLogPattern;

	private final String accessLogPrefix;

	private final String accessLogSuffix;

	private final boolean accessLogRotate;

	private volatile DefaultAccessLogReceiver accessLogReceiver;

	private volatile XnioWorker worker;

	AccessLogHandlerManager(HandlerManager delegate, File accessLogDirectory, String accessLogPattern,
			String accessLogPrefix, String accessLogSuffix, boolean accessLogRotate) {
		this.delegate = delegate;
		this.accessLogDirectory = accessLogDirectory;
		this.accessLogPattern = accessLogPattern;
		this.accessLogPrefix = accessLogPrefix;
		this.accessLogSuffix = accessLogSuffix;
		this.accessLogRotate = accessLogRotate;
	}

	@Override
	public HttpHandler start() {
		HttpHandler handler = this.delegate.start();
		try {
			createAccessLogDirectoryIfNecessary();
			this.worker = createWorker();
			String prefix = (this.accessLogPrefix != null) ? this.accessLogPrefix : "access_log.";
			this.accessLogReceiver = new DefaultAccessLogReceiver(this.worker, this.accessLogDirectory, prefix,
					this.accessLogSuffix, this.accessLogRotate);
			String formatString = (this.accessLogPattern != null) ? this.accessLogPattern : "common";
			return new AccessLogHandler(handler, this.accessLogReceiver, formatString, Undertow.class.getClassLoader());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create AccessLogHandler", ex);
		}
	}

	private void createAccessLogDirectoryIfNecessary() {
		Assert.state(this.accessLogDirectory != null, "Access log directory is not set");
		if (!this.accessLogDirectory.isDirectory() && !this.accessLogDirectory.mkdirs()) {
			throw new IllegalStateException("Failed to create access log directory '" + this.accessLogDirectory + "'");
		}
	}

	private XnioWorker createWorker() throws IOException {
		Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
		return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
	}

	@Override
	public void stop() {
		try {
			this.delegate.stop();
		}
		finally {
			stopAccessLog();
		}
	}

	private void stopAccessLog() {
		try {
			this.accessLogReceiver.close();
			this.worker.shutdown();
			this.worker.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		finally {
			this.delegate.stop();
		}
	}

	@Override
	public <T> T extract(Class<T> type) {
		return this.delegate.extract(type);
	}

}
