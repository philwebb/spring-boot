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

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * A {@link AbstractApplicationContextTester ApplicationContext tester} for a standard,
 * non-web environment {@link ConfigurableApplicationContext}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ApplicationContextTester extends
		AbstractApplicationContextTester<ApplicationContextTester, ConfigurableApplicationContext, AssertableApplicationContext> {

	public ApplicationContextTester() {
		this(AnnotationConfigApplicationContext::new);
	}

	public ApplicationContextTester(
			Supplier<ConfigurableApplicationContext> contextSupplier) {
		super(contextSupplier);
	}

}
