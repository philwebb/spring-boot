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

import javax.servlet.ServletException;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentManager;

/**
 * {@link HandlerManager} that delegates to a {@link DeploymentManager}.
 *
 * @author Andy Wilkinson
 */
class DeploymentManagerHandlerManager implements HandlerManager {

	private final DeploymentManager deploymentManager;

	DeploymentManagerHandlerManager(DeploymentManager deploymentManager) {
		this.deploymentManager = deploymentManager;
	}

	@Override
	public HttpHandler start() {
		try {
			return this.deploymentManager.start();
		}
		catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void stop() {
		try {
			this.deploymentManager.stop();
			this.deploymentManager.undeploy();
		}
		catch (ServletException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T extract(Class<T> type) {
		if (type.equals(DeploymentManager.class)) {
			return (T) this.deploymentManager;
		}
		return null;
	}

}
