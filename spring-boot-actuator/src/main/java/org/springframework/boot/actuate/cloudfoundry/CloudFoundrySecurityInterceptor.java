/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Security interceptor to check cloud foundry token.
 */
class CloudFoundrySecurityInterceptor extends HandlerInterceptorAdapter {

	protected final Log logger = LogFactory.getLog(getClass());

	private final TokenValidator tokenValidator;

	private final CloudFoundrySecurityService cloudFoundrySecurityService;

	private final String applicationId;

	CloudFoundrySecurityInterceptor(TokenValidator tokenValidator,
			CloudFoundrySecurityService cloudFoundrySecurityService,
			String applicationId) {
		this.tokenValidator = tokenValidator;
		this.cloudFoundrySecurityService = cloudFoundrySecurityService;
		this.applicationId = applicationId;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object o) throws Exception {
		try {
			if (!StringUtils.hasText(this.applicationId)) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Application id is not available");
			}
			if (this.cloudFoundrySecurityService == null) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Cloud controller URL is not available");
			}
			HandlerMethod handlerMethod = (HandlerMethod) o;
			MvcEndpoint mvcEndpoint = (MvcEndpoint) handlerMethod.getBean();
			check(request, mvcEndpoint);
			request.setAttribute("cloudFoundryAccessLevel", AccessLevel.FULL);
		}
		catch (CloudFoundryAuthorizationException ex) {
			this.logger.error(ex);
			response.setStatus(ex.getStatusCode().value());
			return false;
		}
		return true;
	}

	private void check(HttpServletRequest request, MvcEndpoint mvcEndpoint)
			throws Exception {
		Token token = getToken(request);
		this.tokenValidator.validate(token);
		AccessLevel accessLevel = this.cloudFoundrySecurityService
				.getAccessLevel(token.toString(), this.applicationId);
		if (!accessLevel.isAccessAllowed(mvcEndpoint.getPath())) {
			throw new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED,
					"Access denied");
		}
	}

	private Token getToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		String bearerPrefix = "bearer ";
		if (authorization == null
				|| !authorization.toLowerCase().startsWith(bearerPrefix)) {
			throw new CloudFoundryAuthorizationException(Reason.MISSING_AUTHORIZATION,
					"Authorization header is missing or invalid");
		}
		return new Token(authorization.substring(bearerPrefix.length()));
	}

}
