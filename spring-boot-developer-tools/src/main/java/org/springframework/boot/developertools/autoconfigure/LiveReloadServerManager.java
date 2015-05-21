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

package org.springframework.boot.developertools.autoconfigure;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.livereload.LiveReloadServer;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class LiveReloadServerManager {

	private static final Log logger = LogFactory.getLog(LiveReloadServerManager.class);

	private LiveReloadServer server;

	public LiveReloadServerManager(LiveReloadServer server) {
		this.server = server;
	}

	@PostConstruct
	public void startServer() throws Exception {
		if (this.server != null) {
			try {
				if (!this.server.isStarted()) {
					this.server.start();
				}
			}
			catch (Exception ex) {
				logger.warn("Unable to start LiveReload server");
				logger.debug("Live reload start error", ex);
				this.server = null;
			}
		}
	}

	@EventListener
	public void onContextRefreshed(ContextRefreshedEvent event) {
		if (this.server != null) {
			this.server.triggerReload();
		}
	}

	@EventListener
	public void onClassPathChanged(ClassPathChangedEvent event) {
		if (!event.isRestartRequired() && this.server != null) {
			this.server.triggerReload();
		}
	}

}
