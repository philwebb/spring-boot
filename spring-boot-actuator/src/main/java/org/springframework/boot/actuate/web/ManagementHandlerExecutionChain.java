/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.web;

import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExecutionChain;

/**
 * A {@link HandlerExecutionChain} for management endpoints.
 * 
 * @author Phillip Webb
 */
public class ManagementHandlerExecutionChain {

	private final HandlerExecutionChain executionChain;

	private final boolean secure;

	public ManagementHandlerExecutionChain(HandlerExecutionChain executionChain) {
		this(executionChain, true);
	}

	public ManagementHandlerExecutionChain(HandlerExecutionChain executionChain,
			boolean secure) {
		Assert.notNull(executionChain, "ExecutionChain must not be null");
		this.executionChain = executionChain;
		this.secure = secure;
	}

	/**
	 * @return the secure
	 */
	public boolean isSecure() {
		return this.secure;
	}

	public HandlerExecutionChain getExecutionChain() {
		return this.executionChain;
	}
}
