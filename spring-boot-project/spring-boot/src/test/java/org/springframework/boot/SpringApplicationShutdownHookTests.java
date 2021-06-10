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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringApplicationShutdownHook}.
 *
 * @author Phillip Webb
 */
class SpringApplicationShutdownHookTests {

	@Test
	void createCallsRegister() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThat(shutdownHook.isRuntimeShutdownHookAdded()).isTrue();
	}

	@Test
	void runClosesContextsBeforeRunningHandlerActions() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		ConfigurableApplicationContext context = new TestApplicationContext(finished);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		Runnable handlerAction = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction);
		shutdownHook.run();
		assertThat(finished).containsExactly(context, handlerAction);
	}

	@Test
	void runWhenContextIsBeingClosedInAnotherThreadWaitsUntilContextIsInactive() throws InterruptedException {
		// This situation occurs in the Spring Tools IDE. It triggers a context close via
		// JMX and then stops the JVM. The two actions happen almost simultaneously
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		CountDownLatch latch = new CountDownLatch(1);
		ConfigurableApplicationContext context = new TestApplicationContext(finished, latch);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		Runnable handlerAction = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction);
		Thread contextThread = new Thread(context::close);
		contextThread.start();
		Thread shutdownThread = new Thread(shutdownHook);
		Thread.sleep(100);
		shutdownThread.start();
		Thread.sleep(100);
		latch.countDown();
		contextThread.join(500);
		shutdownThread.join(500);
		assertThat(finished).containsExactly(context, handlerAction);
	}

	@Test
	void runWhenContextIsClosedDirectlyRunsHandlerActions() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		List<Object> finished = new CopyOnWriteArrayList<>();
		ConfigurableApplicationContext context = new TestApplicationContext(finished);
		shutdownHook.registerApplicationContext(context);
		context.refresh();
		context.close();
		Runnable handlerAction1 = new TestHandlerAction(finished);
		Runnable handlerAction2 = new TestHandlerAction(finished);
		shutdownHook.getHandlers().add(handlerAction1);
		shutdownHook.getHandlers().add(handlerAction2);
		shutdownHook.run();
		assertThat(finished).contains(handlerAction1, handlerAction2);
	}

	@Test
	void addHandlerActionWhenNullThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().add(null))
				.withMessage("Action must not be null");
	}

	@Test
	void addHandlerActionWhenShuttingDownThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		shutdownHook.run();
		Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
		assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().add(handlerAction))
				.withMessage("Shutdown in progress");
	}

	@Test
	void removeHandlerActionWhenNullThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().remove(null))
				.withMessage("Action must not be null");
	}

	@Test
	void removeHandlerActionWhenShuttingDownThrowsException() {
		TestSpringApplicationShutdownHook shutdownHook = new TestSpringApplicationShutdownHook();
		Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
		shutdownHook.getHandlers().add(handlerAction);
		shutdownHook.run();
		assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().remove(handlerAction))
				.withMessage("Shutdown in progress");
	}

	static class TestSpringApplicationShutdownHook extends SpringApplicationShutdownHook {

		private boolean runtimeShutdownHookAdded;

		@Override
		protected void addRuntimeShutdownHook() {
			this.runtimeShutdownHookAdded = true;
		}

		boolean isRuntimeShutdownHookAdded() {
			return this.runtimeShutdownHookAdded;
		}

	}

	static class TestApplicationContext extends AbstractApplicationContext {

		private final ConfigurableListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		private final List<Object> finished;

		private final CountDownLatch latch;

		TestApplicationContext(List<Object> finished) {
			this(finished, null);
		}

		TestApplicationContext(List<Object> finished, CountDownLatch latch) {
			this.finished = finished;
			this.latch = latch;
		}

		@Override
		protected void refreshBeanFactory() {
		}

		@Override
		protected void closeBeanFactory() {
		}

		@Override
		protected void onClose() {
			if (this.latch != null) {
				try {
					this.latch.await(500, TimeUnit.MILLISECONDS);
				}
				catch (InterruptedException ex) {
				}
			}
			this.finished.add(this);
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return this.beanFactory;
		}

	}

	static class TestHandlerAction implements Runnable {

		private final List<Object> finished;

		TestHandlerAction(List<Object> finished) {
			this.finished = finished;
		}

		@Override
		public void run() {
			this.finished.add(this);
		}

	}

}
