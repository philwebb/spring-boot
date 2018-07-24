/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link ServletRegistrationBean} for the auto-configured {@link DispatcherServlet}.
 *
 * @author Phillip Webb
 */
class DispatcherServletRegistrationBean extends ServletRegistrationBean<DispatcherServlet>
		implements DispatcherServletPath {

	private final String path;

	DispatcherServletRegistrationBean(DispatcherServlet servlet, String path) {
		super(servlet);
		this.path = path;
		addUrlMappings(getServletUrlMapping());
	}

	@Override
	public String getPath() {
		return this.path;
	}

}
