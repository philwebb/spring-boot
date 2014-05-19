/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.orm.jpa.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Extended version {@link HibernateJpaVendorAdapter} that provides support for
 * {@link HibernateConfigurationPostProcessor} beans.
 * <p>
 * Requires Hibernate 4.3 or above.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 */
public class ExtendedHibernateJpaVendorAdapter extends HibernateJpaVendorAdapter
		implements ApplicationContextAware {

	private final HibernatePersistenceProvider persistenceProvider;

	private ApplicationContext applicationContext;

	public ExtendedHibernateJpaVendorAdapter() {
		this.persistenceProvider = new ConfigurationPostProcessingHibernatePersistenceProvider();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	protected void postProcessConfiguration(Configuration configuration) {
		List<HibernateConfigurationPostProcessor> beans = new ArrayList<HibernateConfigurationPostProcessor>();
		beans.addAll(this.applicationContext.getBeansOfType(
				HibernateConfigurationPostProcessor.class, true, false).values());
		AnnotationAwareOrderComparator.sort(beans);
		for (HibernateConfigurationPostProcessor bean : beans) {
			bean.postProcessConfiguration(configuration);
		}
	}

	/**
	 * Extended {@link HibernatePersistenceProvider} allowing post processing of the
	 * Hibernate {@link Configuration}.
	 */
	class ConfigurationPostProcessingHibernatePersistenceProvider extends
			HibernatePersistenceProvider {

		@Override
		@SuppressWarnings("rawtypes")
		public EntityManagerFactory createContainerEntityManagerFactory(
				PersistenceUnitInfo info, Map properties) {
			return new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(
					info), properties) {
				@Override
				public Configuration buildHibernateConfiguration(
						ServiceRegistry serviceRegistry) {
					Configuration configuration = super
							.buildHibernateConfiguration(serviceRegistry);
					postProcessConfiguration(configuration);
					return configuration;
				}
			}.build();
		}

	}

}
