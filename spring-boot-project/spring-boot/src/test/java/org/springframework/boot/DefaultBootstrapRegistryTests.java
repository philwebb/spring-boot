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
import java.util.function.Supplier;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.BootstrapRegistry.ApplicationContextPreparedListener;
import org.springframework.boot.BootstrapRegistry.Registration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultBootstrapRegistry}.
 *
 * @author Phillip Webb
 */
class DefaultBootstrapRegistryTests {

	private DefaultBootstrapRegistry registry = new DefaultBootstrapRegistry();

	private AtomicInteger counter = new AtomicInteger();

	private StaticApplicationContext applicationContext = new StaticApplicationContext();

	@Test
	void registerWhenTypeIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.register(null, Registration.of(1)))
				.withMessage("Type must not be null");
	}

	@Test
	void registerWhenRegistrationIsNullThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registry.register(Integer.class, null))
				.withMessage("Registration must not be null");
	}

	@Test
	void registerWhenNotAlreadyRegisteredRegistersInstance() {
		Supplier<Integer> supplier = this.registry.register(Integer.class,
				Registration.suppliedBy(this.counter::getAndIncrement));
		assertThat(supplier.get()).isEqualTo(0);
		assertThat(supplier.get()).isEqualTo(0);
	}

	@Test
	void registerWhenAlreadyRegisteredRegistersReplacedInstance() {
		this.registry.register(Integer.class, Registration.suppliedBy(this.counter::getAndIncrement));
		Supplier<Integer> supplier = this.registry.register(Integer.class, Registration.of(100));
		assertThat(supplier.get()).isEqualTo(100);
		assertThat(supplier.get()).isEqualTo(100);
	}

	@Test
	void registerWhenAlreadyCreatedThrowsException() {
		this.registry.register(Integer.class, Registration.suppliedBy(this.counter::getAndIncrement)).get();
		assertThatIllegalStateException().isThrownBy(() -> this.registry.register(Integer.class, Registration.of(100)))
				.withMessage("java.lang.Integer has already been created");
	}

	@Test
	void registerWithDependencyRegistersInstance() {
		this.registry.register(Integer.class, Registration.of(100));
		String string = this.registry.register(String.class, this::integerAsString).get();
		assertThat(string).isEqualTo("100");
	}

	private String integerAsString(BootstrapRegistry registry) {
		return String.valueOf(registry.get(Integer.class));
	}

	@Test
	void registerIfAbsentWhenAbsentRegisters() {
		Long value = this.registry.registerIfAbsent(Long.class, Registration.of(100L)).get();
		assertThat(value).isEqualTo(100L);
	}

	@Test
	void registerWhenAbsentWhenPresentDoesNotRegister() {
		this.registry.registerIfAbsent(Long.class, Registration.of(1L));
		Long value = this.registry.registerIfAbsent(Long.class, Registration.of(100L)).get();
		assertThat(value).isEqualTo(1L);
	}

	@Test
	void isRegisteredWhenNotRegisteredReturnsFalse() {
		this.registry.register(Number.class, Registration.of(1));
		assertThat(this.registry.isRegistered(Long.class)).isFalse();
	}

	@Test
	void isRegisteredWhenRegisteredReturnsTrue() {
		this.registry.register(Number.class, Registration.of(1));
		assertThat(this.registry.isRegistered(Number.class)).isTrue();
	}

	@Test
	void getRegistrationWhenNotRegisteredReturnsNull() {
		this.registry.register(Number.class, Registration.of(1));
		assertThat(this.registry.getRegistration(Long.class)).isNull();
	}

	@Test
	void getRegistrationWhenRegisteredReturnsRegistration() {
		Registration<Number> registration = Registration.of(1);
		this.registry.register(Number.class, registration);
		assertThat(this.registry.getRegistration(Number.class)).isSameAs(registration);
	}

	@Test
	void getWhenNoRegistrationThrowsIllegalStateException() {
		this.registry.register(Number.class, Registration.of(1));
		assertThatIllegalStateException().isThrownBy(() -> this.registry.get(Long.class))
				.withMessageContaining("has not been registered");
	}

	@Test
	void getWhenRegisteredAsNullReturnsNull() {
		this.registry.register(Number.class, Registration.of(null));
		assertThat(this.registry.get(Number.class)).isNull();
	}

	@Test
	void getCreatesOnlyOneInstance() {
		this.registry.register(Integer.class, Registration.suppliedBy(this.counter::getAndIncrement));
		assertThat(this.registry.get(Integer.class)).isEqualTo(0);
		assertThat(this.registry.get(Integer.class)).isEqualTo(0);
	}

	@Test
	void applicationContextPreparedFiresListeners() {
		TestApplicationContextPreparedListener listener = new TestApplicationContextPreparedListener();
		this.registry.addApplicationContextPreparedListener(listener);
		assertThat(listener).wasNotCalled();
		this.registry.applicationContextPrepared(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce().hasBootstrapRegistrySameAs(this.registry)
				.hasApplicationContextSameAs(this.applicationContext);
	}

	@Test
	void addApplicationContextPreparedListenerIgnoresMultipleCallsWithSameListener() {
		TestApplicationContextPreparedListener listener = new TestApplicationContextPreparedListener();
		this.registry.addApplicationContextPreparedListener(listener);
		this.registry.addApplicationContextPreparedListener(listener);
		this.registry.applicationContextPrepared(this.applicationContext);
		assertThat(listener).wasCalledOnlyOnce();
	}

	private static class TestApplicationContextPreparedListener
			implements ApplicationContextPreparedListener, AssertProvider<ApplicationContextPreparedListenerAssert> {

		private int called;

		private BootstrapRegistry bootstrapRegistry;

		private ConfigurableApplicationContext applicationContext;

		@Override
		public void onApplicationContextPrepared(BootstrapRegistry bootstrapRegistry,
				ConfigurableApplicationContext applicationContext) {
			this.called++;
			this.bootstrapRegistry = bootstrapRegistry;
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

		ApplicationContextPreparedListenerAssert hasBootstrapRegistrySameAs(BootstrapRegistry bootstrapRegistry) {
			assertThat(this.actual.bootstrapRegistry).isSameAs(bootstrapRegistry);
			return this;
		}

		ApplicationContextPreparedListenerAssert hasApplicationContextSameAs(ApplicationContext applicationContext) {
			assertThat(this.actual.applicationContext).isSameAs(applicationContext);
			return this;
		}

	}

}
