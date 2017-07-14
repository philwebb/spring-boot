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

package org.springframework.boot.test.context;

import java.util.function.Supplier;

import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * A {@link AbstractApplicationContextTester ApplicationContext tester} for a Servlet
 * based {@link ConfigurableWebApplicationContext}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class WebApplicationContextTester extends
		AbstractApplicationContextTester<WebApplicationContextTester, ConfigurableWebApplicationContext, AssertableServletWebApplicationContext> {

	public WebApplicationContextTester() {
		this(withMockServletContext(AnnotationConfigWebApplicationContext::new));
	}

	public WebApplicationContextTester(
			Supplier<ConfigurableWebApplicationContext> contextSupplier) {
		super(contextSupplier);
	}

	public static Supplier<ConfigurableWebApplicationContext> withMockServletContext(
			Supplier<ConfigurableWebApplicationContext> contextFactory) {
		return () -> {
			ConfigurableWebApplicationContext context = contextFactory.get();
			context.setServletContext(new MockServletContext());
			return context;
		};
	}
}
