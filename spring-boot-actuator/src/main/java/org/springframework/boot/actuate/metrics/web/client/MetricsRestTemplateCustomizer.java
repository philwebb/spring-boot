/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.client;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * @author awilkinson
 */
public class MetricsRestTemplateCustomizer implements RestTemplateCustomizer {

	@Override
	public void customize(RestTemplate restTemplate) {
		UriTemplateHandler delegate = restTemplate.getUriTemplateHandler();
		restTemplate.setUriTemplateHandler(new UriTemplateHandler() {

			@Override
			public URI expand(String url, Map<String, ?> arguments) {
				RestTemplateUrlTemplateHolder.setRestTemplateUrlTemplate(url);
				return delegate.expand(url, arguments);
			}

			@Override
			public URI expand(String url, Object... arguments) {
				RestTemplateUrlTemplateHolder.setRestTemplateUrlTemplate(url);
				return delegate.expand(url, arguments);
			}

		});

	}

}
