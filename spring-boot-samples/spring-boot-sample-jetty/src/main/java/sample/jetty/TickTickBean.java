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

package sample.jetty;

import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.ProtectionDomain;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

/**
 * @author pwebb
 */
public class TickTickBean implements Lifecycle, InitializingBean {

	private Thready t;

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("Start");

		new RuntimeException("running the ticktick").printStackTrace();

		AccessControlContext context = AccessController.getContext();
		Field field = AccessControlContext.class.getDeclaredField("context");
		field.setAccessible(true);
		ProtectionDomain[] d = (ProtectionDomain[]) field.get(context);
		for (ProtectionDomain protectionDomain : d) {
			System.out.println(protectionDomain);
		}

		this.t = new Thready();
		this.t.start();
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
		System.out.println("Bye");
		this.t.running = false;
		this.t.interrupt();
	}

	@Override
	public boolean isRunning() {
		return this.t.running;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		System.out.println("Tick Tick Final");
	}

	private static class Thready extends Thread {

		boolean running = true;

		@Override
		public void run() {
			while (this.running) {
				System.out.println("Tick");
				try {
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
				}
			}
		}
	}

}
