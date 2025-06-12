/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.autoconfigure.registry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.registry.DefaultHealthContributorRegistry;
import org.springframework.boot.health.registry.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.health.registry.HealthContributorNameValidator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link HealthContributorRegistry} and {@link ReactiveHealthContributorRegistry}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration
public class HealthContributorRegistryAutoConfiguration {

	HealthContributorRegistryAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean(HealthContributorRegistry.class)
	DefaultHealthContributorRegistry healthContributorRegistry(ApplicationContext applicationContext,
			Map<String, HealthContributor> contributorBeans,
			Map<String, ReactiveHealthContributor> reactiveContributorBeans,
			ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
			List<HealthContributorNameValidator> nameValidators) {
		HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
			.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
		Map<String, HealthContributor> contributors = new LinkedHashMap<>();
		contributorBeans.forEach((beanName, bean) -> {
			String contributorName = nameGenerator.generateContributorName(beanName);
			contributors.put(contributorName, bean);
		});
		if (ClassUtils.isPresent("reactor.core.publisher.Flux", applicationContext.getClassLoader())) {
			reactiveContributorBeans.forEach((beanName, bean) -> {
				String contributorName = nameGenerator.generateContributorName(beanName);
				contributors.put(contributorName, bean.asHealthContributor());
			});
		}
		return new DefaultHealthContributorRegistry(contributors, nameValidators);
	}

	static class ReactiveHealthContributorRegistryConfiguration {

		@Bean
		@ConditionalOnMissingBean(ReactiveHealthContributorRegistry.class)
		DefaultReactiveHealthContributorRegistry reactiveHealthContributorRegistry(
				Map<String, ReactiveHealthContributor> reactiveContributorBeans,
				Map<String, HealthContributor> contributorBeans,
				ObjectProvider<HealthContributorNameGenerator> nameGeneratorProvider,
				List<HealthContributorNameValidator> nameValidators) {
			HealthContributorNameGenerator nameGenerator = nameGeneratorProvider
				.getIfAvailable(HealthContributorNameGenerator::withoutStandardSuffixes);
			Map<String, ReactiveHealthContributor> contributors = new LinkedHashMap<>();
			reactiveContributorBeans.forEach((beanName, bean) -> {
				String contributorName = nameGenerator.generateContributorName(beanName);
				contributors.put(contributorName, bean);
			});
			reactiveContributorBeans.forEach((beanName, bean) -> {
				String contributorName = nameGenerator.generateContributorName(beanName);
				contributors.put(contributorName, bean);
			});
			contributorBeans.forEach((beanName, bean) -> {
				String contributorName = nameGenerator.generateContributorName(beanName);
				contributors.computeIfAbsent(contributorName, (key) -> ReactiveHealthContributor.adapt(bean));
			});
			return new DefaultReactiveHealthContributorRegistry(contributors, nameValidators);
		}

	}

}
