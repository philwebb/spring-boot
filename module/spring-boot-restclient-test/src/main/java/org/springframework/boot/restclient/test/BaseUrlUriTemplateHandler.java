/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.test;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.restclient.RootUriTemplateHandler;
import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * {@link RootUriTemplateHandler} based by a {@link BaseUrl}.
 *
 * @author Phillip Webb
 * @since 4.5.0
 */
public class BaseUrlUriTemplateHandler extends RootUriTemplateHandler {

	private final @Nullable BaseUrl baseUrl;

	/**
	 * Create a new {@link BaseUrlUriTemplateHandler} instance.
	 * @param baseUrl the base URL to use
	 */
	public BaseUrlUriTemplateHandler(@Nullable BaseUrl baseUrl) {
		super(new DefaultUriBuilderFactory());
		this.baseUrl = baseUrl;
	}

	@Override
	public @Nullable String getRootUri() {
		Assert.state(this.baseUrl != null, "No base URL available");
		return this.baseUrl.resolve();
	}

}
