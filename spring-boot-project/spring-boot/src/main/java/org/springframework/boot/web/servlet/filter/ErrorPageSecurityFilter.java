/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.servlet.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

/**
 * {@link HttpFilter} that intercepts error dispatches to ensure authorized access to the
 * error page.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @since 2.5.7
 */
public class ErrorPageSecurityFilter extends HttpFilter {

	private WebInvocationPrivilegeEvaluator privilegeEvaluator;

	public ErrorPageSecurityFilter(ApplicationContext context) {
		try {
			this.privilegeEvaluator = context.getBean(WebInvocationPrivilegeEvaluator.class);
		}
		catch (NoSuchBeanDefinitionException ex) {
			this.privilegeEvaluator = null;
		}
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (this.privilegeEvaluator != null) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!this.privilegeEvaluator.isAllowed(request.getRequestURI(), authentication)) {
				sendError(request, response);
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private void sendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Integer errorCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		response.sendError((errorCode != null) ? errorCode : 401);
	}

}
