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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link ExtendedHibernateJpaVendorAdapter}.
 * 
 * @author Phillip Webb
 */
public class ExtendedHibernateJpaVendorAdapterTests {

	@Test
	public void callsPostProcessors() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class);
		PostProcessor postProcessor = context.getBean(PostProcessor.class);
		assertThat(postProcessor.isCalled(), equalTo(true));
		context.getBean(EntityManagerFactory.class).createEntityManager();
		context.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public ExtendedHibernateJpaVendorAdapter jpaVendorAdapter() {
			return new ExtendedHibernateJpaVendorAdapter();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManager() {
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
			entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
			entityManagerFactoryBean.setDataSource(dataSource());
			entityManagerFactoryBean.setPackagesToScan(getClass().getPackage().getName());
			return entityManagerFactoryBean;
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
					.build();
		}

		@Bean
		public PostProcessor postProcessor() {
			return new PostProcessor();
		}

	}

	public static class PostProcessor implements HibernateConfigurationPostProcessor {

		private org.hibernate.cfg.Configuration hibernateConfig;

		@Override
		public void postProcessConfiguration(org.hibernate.cfg.Configuration configuration) {
			this.hibernateConfig = configuration;
		}

		private boolean isCalled() {
			return this.hibernateConfig != null;
		}

	}

}
