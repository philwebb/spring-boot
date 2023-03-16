/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * Abstract base classes for datasource bean post processors which apply values from
 * {@link JdbcServiceConnection}. Acts on beans named 'dataSource' of type {@code T}.
 *
 * @param <T> type of the datasource
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
abstract class AbstractJdbcServiceConnectionBeanPostProcessor<T>
		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware {

	private final Class<T> dataSourceClass;

	private ApplicationContext applicationContext;

	/**
	 * Constructor.
	 * @param dataSourceClass class of the datasource
	 */
	AbstractJdbcServiceConnectionBeanPostProcessor(Class<T> dataSourceClass) {
		this.dataSourceClass = dataSourceClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.dataSourceClass.isAssignableFrom(bean.getClass()) && "dataSource".equals(beanName)) {
			JdbcServiceConnection serviceConnection = this.applicationContext.getBean(JdbcServiceConnection.class);
			return processDataSource((T) bean, serviceConnection);
		}
		return bean;
	}

	protected abstract Object processDataSource(T dataSource, JdbcServiceConnection serviceConnection);

	@Override
	public int getOrder() {
		// Runs after ConfigurationPropertiesBindingPostProcessor
		return Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
