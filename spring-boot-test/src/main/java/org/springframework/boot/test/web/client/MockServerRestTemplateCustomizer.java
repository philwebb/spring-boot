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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
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
 *
 * @author Phillip Webb
 * @see #getServer()
 */
public class MockServerRestTemplateCustomizer implements RestTemplateCustomizer {

	private Lock lock = new ReentrantLock();

	private volatile MockRestServiceServer server;

	private final RequestExpectationManager expectationManager;

	private boolean detectRootUri = true;

	public MockServerRestTemplateCustomizer() {
		this.expectationManager = new SimpleRequestExpectationManager();
	}

	public MockServerRestTemplateCustomizer(
			Class<? extends RequestExpectationManager> expectationManager) {
		this.expectationManager = BeanUtils.instantiate(expectationManager);
	}

	public MockServerRestTemplateCustomizer(
			RequestExpectationManager expectationManager) {
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
		this.lock.lock();
		try {
			RequestExpectationManager expectationManager = this.expectationManager;
			if (this.detectRootUri) {
				expectationManager = RootUriRequestExpectationManager
						.forRestTemplate(restTemplate, expectationManager);
			}
			this.server = MockRestServiceServer.bindTo(restTemplate)
					.build(expectationManager);
		}
		finally {
			this.lock.unlock();
		}
	}

	public MockRestServiceServer getServer() {
		return this.server;
	}

}
