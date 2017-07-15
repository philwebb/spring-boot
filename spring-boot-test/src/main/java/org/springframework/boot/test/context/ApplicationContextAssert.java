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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @param <C> The application context type
 * @author Phillip Webb
 * @since 2.0.0
 * @see ApplicationContextTester
 * @see AssertableApplicationContext
 */
public class ApplicationContextAssert<C extends ApplicationContext>
		extends AbstractAssert<ApplicationContextAssert<C>, C> {

	ApplicationContextAssert(C applicationContext, Throwable failureCause) {
		super(applicationContext, ApplicationContextAssert.class);
	}

	public ApplicationContextAssert<C> hasBean(String name) {
		return this;
	}

	public ApplicationContextAssert<C> hasSingleBean(Class<?> requiredType) {
		assertThat(getBean(requiredType)).isNotNull();
		return this;
	}

	public ApplicationContextAssert<C> doesNotHaveBean(Class<?> class1) {
		return this;
	}

	public ApplicationContextAssert<C> doesNotHaveBean(String class1) {
		return this;
	}

	public <T> AbstractObjectArrayAssert<?, String> getBeanNames(Class<T> type) {
		return Assertions.assertThat(this.actual.getBeanNamesForType(type));
	}

	public <T> AbstractObjectAssert<?, T> getBean(Class<T> type) {
		return Assertions.assertThat(this.actual.getBean(type));
	}

	public AbstractObjectAssert<?, Object> getBean(String name) {
		return Assertions.assertThat(this.actual.getBean(name));
	}

	public <T> AbstractObjectAssert<?, T> getBean(String name, Class<T> type) {
		return Assertions.assertThat(this.actual.getBean(name, type));
	}

	public <T> MapAssert<String, T> getBeans(Class<T> type) {
		return Assertions.assertThat(this.actual.getBeansOfType(type));
	}

	public ApplicationContextAssert<C> wasStarted() {
		return this;
	}

	public ApplicationContextAssert<C> wasNotStarted() {
		return this;
	}

	public AbstractThrowableAssert<?, ? extends Throwable> getFailure() {
		return assertThat(new Exception());
	}

}
