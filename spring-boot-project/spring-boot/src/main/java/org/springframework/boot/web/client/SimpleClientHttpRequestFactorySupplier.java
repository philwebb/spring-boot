/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.client;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * {@link ClientHttpRequestFactorySupplier} for {@link SimpleClientHttpRequestFactory}.
 *
 * @author Andy Wilkinson
 */
class SimpleClientHttpRequestFactorySupplier implements ClientHttpRequestFactorySupplier {

	@Override
	public ClientHttpRequestFactory get(Settings settings) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		if (settings.bufferRequestBody() != null) {
			requestFactory.setBufferRequestBody(settings.bufferRequestBody());
		}
		if (settings.connectTimeout() != null) {
			requestFactory.setConnectTimeout((int) settings.connectTimeout().toMillis());
		}
		if (settings.readTimeout() != null) {
			requestFactory.setReadTimeout((int) settings.readTimeout().toMillis());
		}
		return requestFactory;
	}

}
