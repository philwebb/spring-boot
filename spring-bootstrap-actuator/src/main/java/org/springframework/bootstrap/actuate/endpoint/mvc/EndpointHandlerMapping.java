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
package org.springframework.bootstrap.actuate.endpoint.mvc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.bootstrap.actuate.endpoint.ActionEndpoint;
import org.springframework.bootstrap.actuate.endpoint.Endpoint;
import org.springframework.bootstrap.actuate.properties.ManagementServerProperties;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs. By default {@link Endpoint}s
 * are mapped to a URL by convention from their {@link Endpoint#getId() ID}. Overrides
 * from {@link ManagementServerProperties} are also considered. Standard {@link Endpoint}s
 * are mapped to GET requests, {@link ActionEndpoint}s are mapped to POST requests.
 * 
 * @author Phillip Webb
 * @see EndpointHandlerAdapter
 */
public class EndpointHandlerMapping extends AbstractHandlerMapping {

	public EndpointHandlerMapping() {
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		System.out.println(lookupPath);
		if ("/test".equals(lookupPath)) {
			return new Endpoint<String>() {

				@Override
				public String getId() {
					// TODO Auto-generated method stub
					throw new UnsupportedOperationException("Auto-generated method stub");
				}

				@Override
				public boolean isSensitive() {
					// TODO Auto-generated method stub
					throw new UnsupportedOperationException("Auto-generated method stub");
				}

				@Override
				public MediaType[] produces() {
					return new MediaType[] { MediaType.APPLICATION_JSON };
				}

				@Override
				public String execute() {
					return "{}";
				}
			};
		}
		return null;
	}
}
