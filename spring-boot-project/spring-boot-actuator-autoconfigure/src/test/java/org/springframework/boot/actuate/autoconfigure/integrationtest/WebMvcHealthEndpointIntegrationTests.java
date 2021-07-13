/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Madhura Bhave
 **/
class WebMvcHealthEndpointIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void close() {
		this.context.close();
	}

	@Test
	void groupIsAvailableAtAdditionalPath() throws Exception {
		this.context = new AnnotationConfigServletWebApplicationContext();
		TestPropertyValues.of("management.endpoint.health.group.live.include=diskSpace",
				"management.endpoint.health.show-details=always",
				"management.endpoint.health.group.live.additional-path=server:/healthz").applyTo(this.context);
		this.context.register(WebMvcHealthEndpointIntegrationTests.DefaultConfiguration.class);
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/actuator/health/live").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
		mockMvc.perform(get("/healthz").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
	}

	private MockMvc createMockMvc() {
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		return builder.build();
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			WebMvcAutoConfiguration.class, ManagementContextAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
			DiskSpaceHealthContributorAutoConfiguration.class })
	static class DefaultConfiguration {

	}

}
