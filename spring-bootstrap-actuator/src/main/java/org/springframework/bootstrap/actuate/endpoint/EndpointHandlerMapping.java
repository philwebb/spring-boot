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
package org.springframework.bootstrap.actuate.endpoint;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

public class EndpointHandlerMapping extends AbstractHandlerMapping {

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
