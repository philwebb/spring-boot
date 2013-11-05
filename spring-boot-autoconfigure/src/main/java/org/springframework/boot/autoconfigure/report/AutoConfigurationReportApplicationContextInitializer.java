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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationErrorHandler;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextInitializer} to register the {@link AutoConfigurationReport}
 * bean.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @see AutoConfigurationReport
 */
public class AutoConfigurationReportApplicationContextInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		SpringApplicationErrorHandler {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		beanFactory.registerSingleton(AutoConfigurationReport.BEAN_NAME,
				new AutoConfigurationReport());
	}

	@Override
	public void handleError(SpringApplication application,
			ConfigurableApplicationContext applicationContext, String[] args,
			Throwable exception) {
		if (applicationContext != null) {
			try {
				applicationContext.getBean(AutoConfigurationReport.BEAN_NAME,
						AutoConfigurationReport.class).logErrorReport();
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Swallow and continue
			}
		}
	}

}
