/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.report;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Bean used to gather auto-configuration decisions for reporting purposes.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AutoConfigurationReport implements ApplicationContextAware,
		ApplicationListener<ApplicationEvent> {

	public static final String BEAN_NAME = "autoConfigurationReport";

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent
				&& ((ContextRefreshedEvent) event).getApplicationContext() == this.applicationContext) {
			onContextRefreshed((ContextRefreshedEvent) event);
		}
		if (event instanceof ConditionEvaluationEvent) {
			onConditionEvaluation((ConditionEvaluationEvent) event);
		}

	}

	private void onContextRefreshed(ContextRefreshedEvent event) {
	}

	private void onConditionEvaluation(ConditionEvaluationEvent event) {
	}

	public void logErrorReport() {
	}

}
