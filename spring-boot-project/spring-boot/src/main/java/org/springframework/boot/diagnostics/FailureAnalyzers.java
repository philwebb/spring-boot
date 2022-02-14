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

package org.springframework.boot.diagnostics;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.boot.util.Instantiator;
import org.springframework.boot.util.Instantiator.InstantiationFailureHandler;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code spring.factories}.
 * <p>
 * A {@code FailureAnalyzer} that requires access to the {@link BeanFactory} or
 * {@link Environment} in order to perform its analysis can implement a constructor that
 * accepts arguments of one or both of these types.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
final class FailureAnalyzers implements SpringBootExceptionReporter {

	private static final Log logger = LogFactory.getLog(FailureAnalyzers.class);

	private final ClassLoader classLoader;

	private final List<FailureAnalyzer> analyzers;

	FailureAnalyzers(ConfigurableApplicationContext context) {
		this(SpringFactoriesLoader.loadFactoryNames(FailureAnalyzer.class, getClassLoader(context)), context);
	}

	FailureAnalyzers(List<String> classNames, ConfigurableApplicationContext context) {
		this.classLoader = getClassLoader(context);
		this.analyzers = loadFailureAnalyzers(classNames, context);
	}

	private static ClassLoader getClassLoader(ConfigurableApplicationContext context) {
		return (context != null) ? context.getClassLoader() : null;
	}

	private List<FailureAnalyzer> loadFailureAnalyzers(List<String> classNames,
			ConfigurableApplicationContext context) {
		Instantiator<FailureAnalyzer> instantiator = new Instantiator<>(FailureAnalyzer.class,
				new IgnoringInstantiationFailureHandler(), (availableParameters) -> {
					if (context != null) {
						availableParameters.add(BeanFactory.class, context.getBeanFactory());
						availableParameters.add(Environment.class, context.getEnvironment());
					}
				});
		List<FailureAnalyzer> candidates = instantiator.instantiate(this.classLoader, classNames);
		return candidates.stream().map((analyzer) -> populateAnalyzer(analyzer, context)).filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private FailureAnalyzer populateAnalyzer(FailureAnalyzer analyzer, ConfigurableApplicationContext context) {
		if (analyzer instanceof BeanFactoryAware || analyzer instanceof EnvironmentAware) {
			if (context == null) {
				logger.trace(LogMessage.format("Skipping %s due to missing context", analyzer.getClass().getName()));
				return null;
			}
			if (analyzer instanceof BeanFactoryAware) {
				((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
			}
			if (analyzer instanceof EnvironmentAware) {
				((EnvironmentAware) analyzer).setEnvironment(context.getEnvironment());
			}
		}
		return analyzer;
	}

	@Override
	public boolean reportException(Throwable failure) {
		FailureAnalysis analysis = analyze(failure, this.analyzers);
		return report(analysis, this.classLoader);
	}

	private FailureAnalysis analyze(Throwable failure, List<FailureAnalyzer> analyzers) {
		for (FailureAnalyzer analyzer : analyzers) {
			try {
				FailureAnalysis analysis = analyzer.analyze(failure);
				if (analysis != null) {
					return analysis;
				}
			}
			catch (Throwable ex) {
				logger.trace(LogMessage.format("FailureAnalyzer %s failed", analyzer), ex);
			}
		}
		return null;
	}

	private boolean report(FailureAnalysis analysis, ClassLoader classLoader) {
		List<FailureAnalysisReporter> reporters = SpringFactoriesLoader.loadFactories(FailureAnalysisReporter.class,
				classLoader);
		if (analysis == null || reporters.isEmpty()) {
			return false;
		}
		for (FailureAnalysisReporter reporter : reporters) {
			reporter.report(analysis);
		}
		return true;
	}

	static class IgnoringInstantiationFailureHandler implements InstantiationFailureHandler {

		@Override
		public void handleFailure(Throwable failure, String implementationName, String typeName) {
			if (failure instanceof IllegalAccessException) {
				logger.trace(LogMessage.format("Skipping %s due to no suitable constructor found", implementationName));
			}
			else {
				logger.trace(LogMessage.format("Skipping %s due to exception while creating an instance: %s",
						implementationName, failure.getMessage()));
			}
		}

	}

}
