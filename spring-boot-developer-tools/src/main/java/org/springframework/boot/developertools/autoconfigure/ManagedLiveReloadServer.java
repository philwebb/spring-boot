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
import org.springframework.boot.developertools.livereload.LiveReloadServer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class ManagedLiveReloadServer {

	private static final Log logger = LogFactory.getLog(ManagedLiveReloadServer.class);

	private LiveReloadServer server;

	public ManagedLiveReloadServer(LiveReloadServer server) {
		this.server = server;
	}

	@PostConstruct
	public void startServer() throws Exception {
		if (this.server != null) {
			try {
				if (!this.server.isStarted()) {
					this.server.start();
				}
				logger.info("LiveReload server is running on port "
						+ this.server.getPort());
			}
			catch (Exception ex) {
				logger.warn("Unable to start LiveReload server");
				logger.debug("Live reload start error", ex);
				this.server = null;
			}
		}
	}

	public void triggerReload() {
		if (this.server != null) {
			this.server.triggerReload();
		}
	}

}
