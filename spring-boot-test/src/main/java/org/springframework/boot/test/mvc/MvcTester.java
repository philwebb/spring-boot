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

package org.springframework.boot.test.mvc;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 * @since 1.4.0
 */
public class MvcTester {

	private final MockMvc mvc;

	public MvcTester(MockMvc mvc) {
		Assert.notNull(mvc);
		this.mvc = mvc;
	}

	public MvcServletCall get(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.get(urlTemplate, urlVars));
	}

	public MvcServletCall get(URI uri) {
		return request(MockMvcRequestBuilders.get(uri));
	}

	public MvcServletCall post(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.post(urlTemplate, urlVars));
	}

	public MvcServletCall post(URI uri) {
		return request(MockMvcRequestBuilders.post(uri));
	}

	public MvcServletCall put(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.put(urlTemplate, urlVars));
	}

	public MvcServletCall put(URI uri) {
		return request(MockMvcRequestBuilders.put(uri));
	}

	public MvcServletCall patch(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.patch(urlTemplate, urlVars));
	}

	public MvcServletCall patch(URI uri) {
		return request(MockMvcRequestBuilders.delete(uri));
	}

	public MvcServletCall delete(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.delete(urlTemplate, urlVars));
	}

	public MvcServletCall delete(URI uri) {
		return request(MockMvcRequestBuilders.delete(uri));
	}

	public MvcServletCall options(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.options(urlTemplate, urlVars));
	}

	public MvcServletCall options(URI uri) {
		return request(MockMvcRequestBuilders.options(uri));
	}

	public MvcServletCall head(String urlTemplate, Object... urlVars) {
		return request(MockMvcRequestBuilders.head(urlTemplate, urlVars));
	}

	public MvcServletCall head(URI uri) {
		return request(MockMvcRequestBuilders.head(uri));
	}

	public MvcServletCall request(HttpMethod method, String urlTemplate,
			Object... urlVars) {
		return request(MockMvcRequestBuilders.request(method, urlTemplate, urlVars));
	}

	public MvcServletCall request(HttpMethod method, URI uri) {
		return request(MockMvcRequestBuilders.request(method, uri));
	}

	public MvcServletCall request(String method, URI uri) {
		return request(MockMvcRequestBuilders.request(method, uri));
	}

	public MvcServletCall request(MockHttpServletRequestBuilder requestBuilder) {
		return new MvcServletCall(this.mvc, requestBuilder);
	}

	public MvcOutcome perform(RequestBuilder requestBuilder) {
		try {
			return new MvcOutcome(this.mvc.perform(requestBuilder).andReturn());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
