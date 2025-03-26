/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.NoSuchElementException;

import org.springframework.boot.web.client.RestClientBuilders;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupAdapter;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * Auto-configured {@link RestClientHttpServiceGroupAdapter} that delegates to
 * {@link RestClientBuilders} to resolve groups.
 *
 * @author Phillip Webb
 */
class AutoConfiguredRestClientHttpServiceGroupAdapter extends RestClientHttpServiceGroupAdapter {

	private final RestClientBuilders restClientBuilders;

	AutoConfiguredRestClientHttpServiceGroupAdapter(RestClientBuilders restClientBuilders) {
		this.restClientBuilders = restClientBuilders;
	}

	@Override
	public RestClient.Builder getBaseClientBuilderForGroup(HttpServiceGroup group, ApplicationContext context) {
		try {
			return this.restClientBuilders.get(group.name());
		}
		catch (NoSuchElementException ex) {
			return super.getBaseClientBuilderForGroup(group, context);
		}
	}

}
