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

package org.springframework.boot.test.web.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplateCustomizer} that can be applied to one or more
 * {@link RestTemplateBuilder} instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
 * MyBean bean = new MyBean(new RestTemplateBuilder(customizer));
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * When applied to a single builder the {@link #getServer()} method can be used to get the
 * {@link MockRestServiceServer}, if the same customizer has been applied to several
 * builders the {@link #getServer(RestTemplate)} method must be used.
 *
 * @author Phillip Webb
 */
public class MockServerRestTemplateCustomizer implements RestTemplateCustomizer {

	private Map<RestTemplate, MockRestServiceServer> servers = new ConcurrentHashMap<RestTemplate, MockRestServiceServer>();

	@Override
	public void customize(RestTemplate restTemplate) {
		MockRestServiceServerBuilder builder = MockRestServiceServer.bindTo(restTemplate);
		this.servers.put(restTemplate, builder.build());
	}

	protected void customizeBuilder(MockRestServiceServerBuilder builder) {
	}

	public MockRestServiceServer getServer() {
		return null;
	}

	public MockRestServiceServer getServer(RestTemplate restTemplate) {
		return null;
	}

}
