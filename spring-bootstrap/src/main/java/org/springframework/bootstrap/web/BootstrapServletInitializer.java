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

package org.springframework.bootstrap.web;

import javax.servlet.Filter;

import org.springframework.util.ClassUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.FrameworkServlet;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * A handy opinionated {@link WebApplicationInitializer} for applications that only have
 * one Spring servlet, and no more than a single filter (which itself is only enabled when
 * Spring Security is detected). If your application is more complicated consider using
 * one of the other WebApplicationInitializers.
 * <p>
 * 
 * Note that a WebApplicationInitializer is only needed if you are building a war file and
 * deploying it. If you prefer to run an embedded container (we do) then you won't need
 * this at all.
 * 
 * @author Dave Syer
 */
public abstract class BootstrapServletInitializer extends
		AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return null;
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	@Override
	protected Filter[] getServletFilters() {
		if (ClassUtils.isPresent(
				"org.springframework.security.config.annotation.web.EnableWebSecurity",
				null)) {
			DelegatingFilterProxy filter = new DelegatingFilterProxy(
					"springSecurityFilterChain");
			filter.setContextAttribute(FrameworkServlet.SERVLET_CONTEXT_PREFIX
					+ "dispatcher");
			return new Filter[] { filter };
		}
		return new Filter[0];
	}

}
