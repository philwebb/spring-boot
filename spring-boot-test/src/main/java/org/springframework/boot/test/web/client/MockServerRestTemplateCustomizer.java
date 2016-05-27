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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * {@link RestTemplateCustomizer} that can be applied to a {@link RestTemplateBuilder}
 * instances to add {@link MockRestServiceServer} support.
 * <p>
 * Typically applied to an existing builder before it is used, for example:
 * <pre class="code">
 * MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
 * MyBean bean = new MyBean(new RestTemplateBuilder(customizer));
 * customizer.getServer().expect(requestTo("/hello")).andRespond(withSuccess());
 * bean.makeRestCall();
 * </pre>
 * <p>
 * If the customizer is only used once, the {@link #getServer()} method can be used to
 * obtain the mock server. If the customizer has been used more than once the
 * {@link #getServer(RestTemplate)} or {@link #getServer(int)} method must be used to
 * access the related server.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see #getServer()
 * @see #getServer(RestTemplate)
 */
public class MockServerRestTemplateCustomizer implements RestTemplateCustomizer {

	private Map<RestTemplate, MockRestServiceServer> servers = new ConcurrentHashMap<RestTemplate, MockRestServiceServer>();

	private final Class<? extends RequestExpectationManager> expectationManager;

	private boolean detectRootUri = true;

	public MockServerRestTemplateCustomizer() {
		this.expectationManager = SimpleRequestExpectationManager.class;
	}

	public MockServerRestTemplateCustomizer(
			Class<? extends RequestExpectationManager> expectationManager) {
		this.expectationManager = expectationManager;
	}

	/**
	 * @param detectRootUri the detectRootUri to set
	 */
	public void setDetectRootUri(boolean detectRootUri) {
		this.detectRootUri = detectRootUri;
	}

	@Override
	public void customize(RestTemplate restTemplate) {
		RequestExpectationManager expectationManager = createExpecationManager();
		if (this.detectRootUri) {
			expectationManager = RootUriRequestExpectationManager
					.forRestTemplate(restTemplate, expectationManager);
		}
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate)
				.build(expectationManager);
		this.servers.put(restTemplate, server);
	}

	protected RequestExpectationManager createExpecationManager() {
		return BeanUtils.instantiate(this.expectationManager);
	}

	public MockRestServiceServer getServer() {
		if (this.servers.isEmpty()) {
			return null;
		}
		Assert.state(this.servers.size() == 1,
				"Unable to return a single MockRestServiceServer since "
						+ "MockServerRestTemplateCustomizer has been bound to "
						+ "more than one RestTemplate");
		return this.servers.values().iterator().next();
	}

	public MockRestServiceServer getServer(RestTemplate restTemplate) {
		return this.servers.get(restTemplate);
	}

	public MockRestServiceServer getServer(int index) {
		for (Map.Entry<RestTemplate, MockRestServiceServer> entry : this.servers
				.entrySet()) {
			if (index == 0) {
				return entry.getValue();
			}
			index--;
		}
		return null;

	}

	public Map<RestTemplate, MockRestServiceServer> getServers() {
		return Collections.unmodifiableMap(this.servers);
	}

}
