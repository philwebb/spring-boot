/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author pwebb
 */
public class TimeMe {

	public static void main(String[] args) throws Exception {
		MutablePropertySources propertySources = new MutablePropertySources();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("server.port", "123");
		propertySources.addFirst(new MapPropertySource("test", map));
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		validator.setApplicationContext(context);
		validator.setMessageInterpolator(interpolatorFactory.getObject());
		validator.afterPropertiesSet();

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigurationProperties.class));
		Set<BeanDefinition> components = scanner
				.findCandidateComponents("org.springframework.boot");
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		for (BeanDefinition beanDefinition : components) {
			String name = beanDefinition.getBeanClassName();
			Class<?> resolveClassName = ClassUtils.resolveClassName(name, null);
			try {
				BeanUtils.instantiate(resolveClassName);
				classes.add(resolveClassName);
			}
			catch (BeanInstantiationException ex) {
			}
		}

		long t = System.nanoTime();
		for (Class<?> class1 : classes) {
			PropertiesConfigurationFactory<?> factory = new PropertiesConfigurationFactory<Object>(
					class1);
			factory.setPropertySources(propertySources);
			factory.setValidator(validator);
			try {
				factory.getObject();
			}
			catch (Exception ex) {
			}
		}
		System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t));
	}

}
