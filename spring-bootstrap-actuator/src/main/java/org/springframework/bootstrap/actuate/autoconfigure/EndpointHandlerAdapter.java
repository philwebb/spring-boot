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
package org.springframework.bootstrap.actuate.autoconfigure;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.bootstrap.actuate.Endpoint;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Phillip Webb
 */
public class EndpointHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Endpoint);
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		return 0;
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		// FIXME implement
		response.getOutputStream().write("Hello".getBytes());
		response.flushBuffer();
		return null;
	}

}
