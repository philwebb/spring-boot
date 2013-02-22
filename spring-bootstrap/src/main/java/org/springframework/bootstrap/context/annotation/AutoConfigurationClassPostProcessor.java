/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.context.annotation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Variant of {@link ConfigurationClassPostProcessor} that can auto configure the
 * application context.
 *
 * @author Phillip Webb
 */
@Deprecated
public class AutoConfigurationClassPostProcessor extends ConfigurationClassPostProcessor {

	//FIXME DELETE

	private static final String DISABLE_AUTO_CONFIGURATION_ANNOTATION = DisableAutoConfiguration.class.getName();

	private static final String AUTO_CONFIGURATION_ANNOTATION = AutoConfiguration.class.getName();

	public static final String AUTO_CONFIGURATION_BEAN_NAME = "org.springframework.bootstrap.autoconfigure.internalAutoConfiguration";

	public static final String DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE = "disabledAutoConfigrations";

	private final Log logger = LogFactory.getLog(getClass());

	@Override
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		registerAutoConfigurationBean(registry);
		super.processConfigBeanDefinitions(registry);
	}

	/**
	 * This method is called before processing configuration beans to ensure
	 * the {@link AutoConfigurationBean} is registered below all other beans.
	 * We only attempt auto-configuration when the user has had their say.
	 * @param registry
	 */
	private void registerAutoConfigurationBean(BeanDefinitionRegistry registry) {
		// Ensure the auto configure bean is always the last bean
		if (registry.containsBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME)) {
			registry.removeBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME);
		}
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(AutoConfigurationBean.class);
		registry.registerBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME,
				beanDefinition);
	}

	@Override
	protected void afterLoadConfiguration(BeanDefinitionRegistry registry,
			String beanName, AnnotationMetadata metadata) {
		super.afterLoadConfiguration(registry, beanName, metadata);
		if(metadata.isAnnotated(DISABLE_AUTO_CONFIGURATION_ANNOTATION)) {
			Map<String, Object> annotation = metadata.getAnnotationAttributes(DISABLE_AUTO_CONFIGURATION_ANNOTATION, true);
			String[] annotationValue = (String[]) annotation.get("value");
			for (String configurationClass : annotationValue) {
				if(logger.isInfoEnabled()) {
					logger.info("Disabling auto configuration "+configurationClass);
				}
				addDisabledAutoConfiguration(registry, configurationClass);
			}
		}
		if(metadata.isAnnotated(AUTO_CONFIGURATION_ANNOTATION)) {
			if(logger.isInfoEnabled()) {
				logger.info("Applying auto configuration "+metadata.getClassName());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addDisabledAutoConfiguration(BeanDefinitionRegistry registry, String configurationClass) {
		BeanDefinition definition = registry.getBeanDefinition(AUTO_CONFIGURATION_BEAN_NAME);
		Set<String> disabled = (Set<String>) definition.getAttribute(DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE);
		if(disabled == null) {
			disabled = new HashSet<String>();
			definition.setAttribute(DISABLED_AUTO_CONFIGURATIONS_ATTRIBUTE, disabled);
		}
		disabled.add(configurationClass);
	}

	/**
	 * Internal configuration class used to start the auto-configure process.  This
	 * bean simply delegates to the {@link AutoConfigurationImports}.
	 */
	@Configuration
	@Import(AutoConfigurationImports.class)
	public static class AutoConfigurationBean {
	}

	/**
	 * Imports all auto-configuration beans.
	 */
	public static class AutoConfigurationImports implements ImportSelector {
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(AutoConfiguration.class, getClass().getClassLoader());
			return factoryNames.toArray(new String[factoryNames.size()]);
		}
	}

}
