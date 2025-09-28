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

package org.springframework.boot.web.server.client;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient.Builder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@link WebTestClientBuilderCustomizer} to set the base path from the running the web
 * server.
 *
 * @author Phillip Webb
 */
class BasePathWebTestClientBuilderCustomizer implements WebTestClientBuilderCustomizer {

	private static final String SERVLET_APPLICATION_CONTEXT_CLASS = "org.springframework.web.context.WebApplicationContext";

	private static final String REACTIVE_APPLICATION_CONTEXT_CLASS = "org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext";

	private final ApplicationContext context;

	BasePathWebTestClientBuilderCustomizer(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void customize(Builder builder) {
		builder.baseUrl(getBaseUrl());
	}

	private String getBaseUrl() {
		String port = this.context.getEnvironment().getProperty("local.server.port", "8080");
		String basePath = deduceBasePath();
		String path = (StringUtils.hasText(basePath)) ? basePath : "";
		return (isSslEnabled() ? "https" : "http") + "://localhost:" + port + path;
	}

	private @Nullable String deduceBasePath() {
		WebApplicationType webApplicationType = deduceWebApplicationType();
		if (webApplicationType == WebApplicationType.REACTIVE) {
			return this.context.getEnvironment().getProperty("spring.webflux.base-path");
		}
		if (webApplicationType == WebApplicationType.SERVLET) {
			ServletContext servletContext = ((WebApplicationContext) this.context).getServletContext();
			Assert.state(servletContext != null, "'servletContext' must not be null");
			return servletContext.getContextPath();
		}
		return null;
	}

	private WebApplicationType deduceWebApplicationType() {
		Class<?> contextClass = this.context.getClass();
		if (isAssignable(SERVLET_APPLICATION_CONTEXT_CLASS, contextClass)) {
			return WebApplicationType.SERVLET;
		}
		if (isAssignable(REACTIVE_APPLICATION_CONTEXT_CLASS, contextClass)) {
			return WebApplicationType.REACTIVE;
		}
		return WebApplicationType.NONE;
	}

	private static boolean isAssignable(String target, Class<?> type) {
		try {
			return ClassUtils.resolveClassName(target, null).isAssignableFrom(type);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	private boolean isSslEnabled() {
		try {
			AbstractReactiveWebServerFactory webServerFactory = this.context
				.getBean(AbstractReactiveWebServerFactory.class);
			return webServerFactory.getSsl() != null && webServerFactory.getSsl().isEnabled();
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

}
