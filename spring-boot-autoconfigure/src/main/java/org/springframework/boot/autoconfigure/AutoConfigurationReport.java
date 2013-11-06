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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.Condition;

/**
 * Special Spring Bean that records configuration outcomes for reporting against.
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 */
public class AutoConfigurationReport {

	private static final String BEAN_NAME = "autoConfigurationReport";

	private final SortedMap<String, ConditionOutcomes> outcomes = new TreeMap<String, ConditionOutcomes>();

	public void recordConditionEvaluation(Condition condition, String target,
			ConditionOutcome outcome) {
		if (!this.outcomes.containsKey(target)) {
			this.outcomes.put(target, new ConditionOutcomes());
		}
		this.outcomes.get(target).add(condition, outcome);
	}

	public Map<String, ConditionOutcomes> getOutcomes() {
		return Collections.unmodifiableMap(this.outcomes);
	}

	public static AutoConfigurationReport get(ConfigurableListableBeanFactory beanFactory) {
		synchronized (beanFactory) {
			try {
				return beanFactory.getBean(BEAN_NAME, AutoConfigurationReport.class);
			}
			catch (NoSuchBeanDefinitionException ex) {
				AutoConfigurationReport report = new AutoConfigurationReport();
				beanFactory.registerSingleton(BEAN_NAME, report);
				return report;
			}
		}
	}

	public static class ConditionOutcomes implements Iterable<ConditionAndOutcome> {

		private List<ConditionAndOutcome> outcomes = new ArrayList<ConditionAndOutcome>();

		public void add(Condition condition, ConditionOutcome outcome) {
			this.outcomes.add(new ConditionAndOutcome(condition, outcome));
		}

		boolean isFullMatch() {
			for (ConditionAndOutcome conditionAndOutcomes : this) {
				if (!conditionAndOutcomes.getOutcome().isMatch()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Iterator<ConditionAndOutcome> iterator() {
			return Collections.unmodifiableList(this.outcomes).iterator();
		}

	}

	public static class ConditionAndOutcome {

		private final Condition condition;

		private final ConditionOutcome outcome;

		public ConditionAndOutcome(Condition condition, ConditionOutcome outcome) {
			this.condition = condition;
			this.outcome = outcome;
		}

		public Condition getCondition() {
			return this.condition;
		}

		public ConditionOutcome getOutcome() {
			return this.outcome;
		}
	}

}
