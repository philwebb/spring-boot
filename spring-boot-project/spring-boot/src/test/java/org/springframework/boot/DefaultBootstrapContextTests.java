/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry.ApplicationContextPreparedListener;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultBootstrapContext}.
 *
 * @author Phillip Webb
 */
class DefaultBootstrapContextTests {

	private DefaultBootstrapContext context = new DefaultBootstrapContext();

	private AtomicInteger counter = new AtomicInteger();

	private StaticApplicationContext applicationContext = new StaticApplicationContext();

	@Test
	void registerWhenTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(null, InstanceSupplier.of(1)))
				.withMessage("Type must not be null");
	}

	@Test
	void registerWhenRegistrationIsNullThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.context.register(Integer.class, null))
				.withMessage("Registration must not be null");
	}

	@Test
	void registerWhenNotAlreadyRegisteredRegistersInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.get(Integer.class)).isEqualTo(0);
		assertThat(this.context.get(Integer.class)).isEqualTo(0);
	}

	@Test
	void registerWhenAlreadyRegisteredRegistersReplacedInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		this.context.register(Integer.class, InstanceSupplier.of(100));
		assertThat(this.context.get(Integer.class)).isEqualTo(100);
		assertThat(this.context.get(Integer.class)).isEqualTo(100);
	}

	@Test
	void registerWhenAlreadyCreatedThrowsException() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		this.context.get(Integer.class);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.context.register(Integer.class, InstanceSupplier.of(100)))
				.withMessage("java.lang.Integer has already been created");
	}

	@Test
	void registerWithDependencyRegistersInstance() {
		this.context.register(Integer.class, InstanceSupplier.of(100));
		this.context.register(String.class, this::integerAsString);
		assertThat(this.context.get(String.class)).isEqualTo("100");
	}

	private String integerAsString(BootstrapContext context) {
		return String.valueOf(context.get(Integer.class));
	}

	@Test
	void registerIfAbsentWhenAbsentRegisters() {
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
		assertThat(this.context.get(Long.class)).isEqualTo(100L);
	}

	@Test
	void registerWhenAbsentWhenPresentDoesNotRegister() {
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(1L));
		this.context.registerIfAbsent(Long.class, InstanceSupplier.of(100L));
		assertThat(this.context.get(Long.class)).isEqualTo(1L);
	}

	@Test
	void isRegisteredWhenNotRegisteredReturnsFalse() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.isRegistered(Long.class)).isFalse();
	}

	@Test
	void isRegisteredWhenRegisteredReturnsTrue() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.isRegistered(Number.class)).isTrue();
	}

	@Test
	void getRegistrationWhenNotRegisteredReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThat(this.context.getRegisteredInstanceSupplier(Long.class)).isNull();
	}

	@Test
	void getRegistrationWhenRegisteredReturnsRegistration() {
		InstanceSupplier<Number> instanceSupplier = InstanceSupplier.of(1);
		this.context.register(Number.class, instanceSupplier);
		assertThat(this.context.getRegisteredInstanceSupplier(Number.class)).isSameAs(instanceSupplier);
	}

	@Test
	void getWhenNoRegistrationThrowsIllegalStateException() {
		this.context.register(Number.class, InstanceSupplier.of(1));
		assertThatIllegalStateException().isThrownBy(() -> this.context.get(Long.class))
				.withMessageContaining("has not been registered");
	}

	@Test
	void getWhenRegisteredAsNullReturnsNull() {
		this.context.register(Number.class, InstanceSupplier.of(null));
		assertThat(this.context.get(Number.class)).isNull();
	}

	@Test
	void getCreatesOnlyOneInstance() {
		this.context.register(Integer.class, InstanceSupplier.from(this.counter::getAndIncrement));
		assertThat(this.context.get(Integer.class)).isEqualTo(0);
		assertThat(this.context.get(Integer.class)).isEqualTo(0);
	}

	@Test
	void applicationContextPreparedFiresListeners() {
		TestApplicationContextPreparedListener listener = new TestApplicationContextPreparedListener();
		this.context.addApplicationContextPreparedListener(listener);
		assertThat(listener).wasNotCalled();
		this.context.applicationContextPrepared(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce().hasBootstrapContextSameAs(this.context)
				.hasApplicationContextSameAs(this.applicationContext);
	}

	@Test
	void addApplicationContextPreparedListenerIgnoresMultipleCallsWithSameListener() {
		TestApplicationContextPreparedListener listener = new TestApplicationContextPreparedListener();
		this.context.addApplicationContextPreparedListener(listener);
		this.context.addApplicationContextPreparedListener(listener);
		this.context.applicationContextPrepared(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce();
	}

	private static class TestApplicationContextPreparedListener
			implements ApplicationContextPreparedListener, AssertProvider<ApplicationContextPreparedListenerAssert> {

		private int called;

		private BootstrapContext bootstrapContext;

		private ConfigurableApplicationContext applicationContext;

		@Override
		public void onApplicationContextPrepared(BootstrapContext bootstrapContext,
				ConfigurableApplicationContext applicationContext) {
			this.called++;
			this.bootstrapContext = bootstrapContext;
			this.applicationContext = applicationContext;
		}

		@Override
		public ApplicationContextPreparedListenerAssert assertThat() {
			return new ApplicationContextPreparedListenerAssert(this);
		}

	}

	private static class ApplicationContextPreparedListenerAssert
			extends AbstractAssert<ApplicationContextPreparedListenerAssert, TestApplicationContextPreparedListener> {

		ApplicationContextPreparedListenerAssert(TestApplicationContextPreparedListener actual) {
			super(actual, ApplicationContextPreparedListenerAssert.class);
		}

		ApplicationContextPreparedListenerAssert wasCalledOnlyOnce() {
			assertThat(this.actual.called).as("action calls").isEqualTo(1);
			return this;
		}

		ApplicationContextPreparedListenerAssert wasNotCalled() {
			assertThat(this.actual.called).as("action calls").isEqualTo(0);
			return this;
		}

		ApplicationContextPreparedListenerAssert hasBootstrapContextSameAs(BootstrapContext bootstrapContext) {
			assertThat(this.actual.bootstrapContext).isSameAs(bootstrapContext);
			return this;
		}

		ApplicationContextPreparedListenerAssert hasApplicationContextSameAs(ApplicationContext applicationContext) {
			assertThat(this.actual.applicationContext).isSameAs(applicationContext);
			return this;
		}

	}

}
