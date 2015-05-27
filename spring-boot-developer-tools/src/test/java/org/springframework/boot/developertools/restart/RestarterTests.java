/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.restart;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.developertools.autoconfigure.MockRestartInitializer;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link Restarter}.
 *
 * @author Phillip Webb
 */
public class RestarterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture out = new OutputCapture();

	@Before
	@After
	public void cleanup() {
		Restarter.clearInstance();
	}

	@Test
	public void cantGetInstanceBeforeInitialize() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Restarter has not been initialized");
		Restarter.getInstance();
	}

	@Test
	public void testRestart() throws Exception {
		Thread thread = new Thread() {

			@Override
			public void run() {
				SampleApplication.main();
			};

		};
		thread.start();
		Thread.sleep(1400);
		String output = this.out.toString();
		assertThat(StringUtils.countOccurrencesOf(output, "Tick 0"), greaterThan(2));
		assertThat(StringUtils.countOccurrencesOf(output, "Tick 1"), greaterThan(2));
	}

	@Component
	@EnableScheduling
	public static class SampleApplication {

		private int count = 0;

		private static volatile boolean quit = false;

		@Scheduled(fixedDelay = 100)
		public void tickBean() {
			System.out.println("Tick " + this.count++ + " " + Thread.currentThread());
		}

		@Scheduled(initialDelay = 350, fixedDelay = 350)
		public void restart() {
			System.out.println("Restart " + Thread.currentThread());
			if (!SampleApplication.quit) {
				Restarter.getInstance().restart();
			}
		}

		public static void main(String... args) {
			Restarter.initialize(args, false, new MockRestartInitializer());
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
					SampleApplication.class);
			context.registerShutdownHook();
			System.out.println("Sleep " + Thread.currentThread());
			sleep();
			quit = true;
			context.close();
		}

		private static void sleep() {
			try {
				Thread.sleep(1200);
			}
			catch (InterruptedException ex) {
			}
		}

	}

	// FIXME test add files add URLs

}
