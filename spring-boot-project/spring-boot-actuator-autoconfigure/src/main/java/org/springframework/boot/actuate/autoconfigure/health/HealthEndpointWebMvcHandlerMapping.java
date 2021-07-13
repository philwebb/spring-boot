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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.servlet.AbstractWebMvcEndpointHandlerMapping;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A custom {@link HandlerMapping} that allows health groups to be mapped to an additional
 * path.
 *
 * @author Madhura Bhave
 **/
public class HealthEndpointWebMvcHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final Set<HealthEndpointGroup> groups;

	private ExposableWebEndpoint endpoint;

	public HealthEndpointWebMvcHandlerMapping(EndpointMapping endpointMapping, ExposableWebEndpoint endpoint,
			EndpointMediaTypes endpointMediaTypes, boolean shouldRegisterLinksMapping,
			Set<HealthEndpointGroup> groups) {
		super(endpointMapping, Collections.singletonList(endpoint), endpointMediaTypes, shouldRegisterLinksMapping);
		this.endpoint = endpoint;
		this.groups = groups;
	}

	@Override
	protected void initHandlerMethods() {
		for (WebOperation operation : this.endpoint.getOperations()) {
			WebOperationRequestPredicate predicate = operation.getRequestPredicate();
			String matchAllRemainingPathSegmentsVariable = predicate.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				for (HealthEndpointGroup group : this.groups) {
					String path = group.getAdditionalPath();
					registerMapping(this.endpoint, predicate, operation, path.substring(path.indexOf(":") + 1));
				}
			}
		}
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return null;
	}

}
