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

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Interface to be implemented by objects that define a mapping between management
 * requests and handler objects. Similar to the standard {@link HttpServletRequest} but
 * only considered for management requests (which could on a different port or restricted
 * to a specific path).
 * 
 * @author Phillip Webb
 * @see HandlerMapping
 */
public interface ManagementHandlerMapping {

	/**
	 * Return a handler and any interceptors for this request. The choice may be made on
	 * request URL, session state, or any factor the implementing class chooses.
	 * <p>
	 * The returned HandlerExecutionChain contains a handler Object, rather than even a
	 * tag interface, so that handlers are not constrained in any way. For example, a
	 * HandlerAdapter could be written to allow another framework's handler objects to be
	 * used.
	 * <p>
	 * Returns {@code null} if no match was found. This is not an error. The
	 * DispatcherServlet will query all registered HandlerMapping beans to find a match,
	 * and only decide there is an error if none can find a handler.
	 * @param managementServerProperties the management server properties, often the
	 * {@link ManagementServerProperties#getContextPath() context path} will need to be
	 * considered when mapping requests.
	 * @param request current HTTP request
	 * @return a {@link ManagementHandlerExecutionChain}, or {@code null} if no mapping
	 * found
	 * @throws Exception if there is an internal error
	 * @see {@link HandlerMapping#getHandler(HttpServletRequest)}
	 */
	ManagementHandlerExecutionChain getHandler(
			ManagementServerProperties managementServerProperties,
			HttpServletRequest request) throws Exception;

}
