/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import javax.validation.Validation;

import org.apache.catalina.mbeans.MBeanFactory;
import org.thymeleaf.spring4.SpringTemplateEngine;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;

/**
 * {@link ApplicationListener} to trigger early initialization in a background thread of
 * time consuming tasks.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Order(LoggingApplicationListener.DEFAULT_ORDER + 1)
public class BackgroundPreinitializer
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		try {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					runSafely(new MessageConverterInitializer());
					runSafely(new MBeanFactoryInitializer());
					runSafely(new ValidationInitializer());
					runSafely(new JacksonInitializer());
					runSafely(new ConversionServiceInitializer());
					runSafely(new ThymeleafSpringTemplateEngineInitializer());
				}

				public void runSafely(Runnable runnable) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						// Ignore
					}
				}

			}, "background-preinit");
			thread.start();
		}
		catch (Exception ex) {
			// This will fail on GAE where creating threads is prohibited. We can safely
			// continue but startup will be slightly slower as the initialization will now
			// happen on the main thread.
		}
	}

	/**
	 * Early initializer for Spring MessageConverters.
	 */
	private static class MessageConverterInitializer implements Runnable {

		@Override
		public void run() {
			new AllEncompassingFormHttpMessageConverter();
		}

	}

	/**
	 * Early initializer to load Tomcat MBean XML.
	 */
	private static class MBeanFactoryInitializer implements Runnable {

		@Override
		public void run() {
			new MBeanFactory();
		}

	}

	/**
	 * Early initializer for javax.validation.
	 */
	private static class ValidationInitializer implements Runnable {

		@Override
		public void run() {
			Validation.byDefaultProvider().configure();
			Validation.buildDefaultValidatorFactory().getValidator();
		}

	}

	/**
	 * Early initializer for Jackson.
	 */
	private static class JacksonInitializer implements Runnable {

		@Override
		public void run() {
			Jackson2ObjectMapperBuilder.json().build();
		}

	}

	/**
	 * Early initializer for Spring's ConversionService.
	 */
	private static class ConversionServiceInitializer implements Runnable {

		@Override
		public void run() {
			new DefaultFormattingConversionService();
		}

	}

	private static class ThymeleafSpringTemplateEngineInitializer implements Runnable {

		@Override
		public void run() {
			new SpringTemplateEngine();
		}

	}

}
