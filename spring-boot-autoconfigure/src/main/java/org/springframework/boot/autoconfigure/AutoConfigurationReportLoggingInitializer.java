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

package org.springframework.boot.autoconfigure;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationErrorHandler;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.AutoConfigurationReport.ConditionOutcomes;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} and {@link SpringApplicationErrorHandler} that
 * prints the {@link AutoConfigurationReport} to the console. This initializer is not
 * intended to be shared accross multiple application context instances.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AutoConfigurationReportLoggingInitializer implements
		ApplicationContextInitializer<ConfigurableApplicationContext>,
		SpringApplicationErrorHandler {

	private static final String LOGGER_BEAN = "autoConfigurationReportLogger";

	private AutoConfigurationReportLogger loggerBean;

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		this.loggerBean = new AutoConfigurationReportLogger(applicationContext);
		applicationContext.getBeanFactory().registerSingleton(LOGGER_BEAN,
				this.loggerBean);
	}

	@Override
	public void handleError(SpringApplication application,
			ConfigurableApplicationContext applicationContext, String[] args,
			Throwable exception) {
		if (this.loggerBean != null) {
			this.loggerBean.logAutoConfigurationReport(true);
		}
	}

	public static class AutoConfigurationReportLogger implements
			ApplicationListener<ContextRefreshedEvent> {

		private final Log logger = LogFactory.getLog(getClass());

		private final ConfigurableApplicationContext applicationContext;

		private final AutoConfigurationReport report;

		public AutoConfigurationReportLogger(
				ConfigurableApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
			// Get the report early
			this.report = AutoConfigurationReport.get(this.applicationContext
					.getBeanFactory());

		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			if (event.getApplicationContext() == this.applicationContext) {
				logAutoConfigurationReport();
			}
		}

		private void logAutoConfigurationReport() {
			logAutoConfigurationReport(!this.applicationContext.isActive());
		}

		void logAutoConfigurationReport(boolean isCrashReport) {
			if (this.report.getOutcomes().size() > 0) {
				if (isCrashReport && this.logger.isInfoEnabled()) {
					this.logger.info(getLogMessage(this.report.getOutcomes()));
				}
				else if (!isCrashReport && this.logger.isDebugEnabled()) {
					this.logger.debug(getLogMessage(this.report.getOutcomes()));
				}
			}
		}

		private StringBuilder getLogMessage(Map<String, ConditionOutcomes> outcomes) {
			StringBuilder message = new StringBuilder();
			message.append("\n\n\n");
			message.append("=========================\n");
			message.append("AUTO-CONFIGURATION REPORT\n");
			message.append("=========================\n\n\n");
			message.append("Positive matches:\n");
			message.append("-----------------\n");
			for (Map.Entry<String, ConditionOutcomes> entry : outcomes.entrySet()) {
				if (entry.getValue().isFullMatch()) {
					addLogMessage(message, entry.getKey(), entry.getValue());
				}
			}
			message.append("\n\n");
			message.append("Negative matches:\n");
			message.append("-----------------\n");
			for (Map.Entry<String, ConditionOutcomes> entry : outcomes.entrySet()) {
				if (!entry.getValue().isFullMatch()) {
					addLogMessage(message, entry.getKey(), entry.getValue());
				}
			}
			message.append("\n\n");
			return message;
		}

		private void addLogMessage(StringBuilder message, String name,
				ConditionOutcomes conditionAndOutcomes) {
			message.append("\n   " + ClassUtils.getShortName(name) + "\n");
			for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
				message.append("      - ");
				Class<?> conditionClass = conditionAndOutcome.getCondition().getClass();
				if (StringUtils.hasLength(conditionAndOutcome.getOutcome().getMessage())) {
					message.append(conditionAndOutcome.getOutcome().getMessage());
				}
				else {
					message.append(conditionAndOutcome.getOutcome().isMatch() ? "matched"
							: "did not match");
				}
				message.append(" (");
				message.append(ClassUtils.getShortName(conditionClass));
				message.append(")\n");
			}
		}
	}

}
