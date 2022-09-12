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

package org.springframework.boot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotInitializer;

/**
 * A {@link ApplicationContextInitializer} wrapper used to initialize a
 * {@link ConfigurableApplicationContext} using artifacts that were generated
 * ahead-of-time.
 *
 * @param <C> the application context type
 * @author Phillip Webb
 * @since 3.0.0
 */
public final class AotApplicationContextInitializer<C extends ConfigurableApplicationContext>
		implements ApplicationContextInitializer<C> {

	private ApplicationContextInitializer<C> initializer;

	public AotApplicationContextInitializer(String name, ApplicationContextInitializer<C> initializer) {
		this.initializer = initializer;
	}

	@Override
	public void initialize(C applicationContext) {
		this.initializer.initialize(applicationContext);
	}

	static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> forMainApplicationClass(
			Class<?> mainApplicationClass) {
		// FIXME roll up logic from ApplicationContextAotInitializer
		String className = mainApplicationClass.getName() + "__ApplicationContextInitializer";
		return new AotApplicationContextInitializer<>(className,
				(context) -> new ApplicationContextAotInitializer().initialize(context, className));
	}

	public static <C extends ConfigurableApplicationContext> AotApplicationContextInitializer<C> of(
			ApplicationContextInitializer<C> initializer) {
		return new AotApplicationContextInitializer<>(initializer.getClass().getName(), initializer);
	}

}
