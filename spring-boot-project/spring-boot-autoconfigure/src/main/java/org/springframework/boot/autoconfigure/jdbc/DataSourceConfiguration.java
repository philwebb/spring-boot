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

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSourceImpl;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Fabio Grassi
 * @author Moritz Halbritter
 */
abstract class DataSourceConfiguration {

	@SuppressWarnings("unchecked")
	protected static <T> T createDataSource(DataSourceProperties properties, Class<? extends DataSource> type) {
		return (T) properties.initializeDataSourceBuilder().type(type).build();
	}

	@SuppressWarnings("unchecked")
	protected static <T> T createDataSource(JdbcServiceConnection serviceConnection, Class<? extends DataSource> type,
			ClassLoader classLoader) {
		return (T) DataSourceBuilder.create(classLoader)
			.url(serviceConnection.getJdbcUrl())
			.username(serviceConnection.getUsername())
			.password(serviceConnection.getPassword())
			.type(type)
			.build();
	}

	/**
	 * Tomcat Pool DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource",
			matchIfMissing = true)
	static class Tomcat implements BeanClassLoaderAware {

		private ClassLoader classLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Bean
		@ConditionalOnBean(JdbcServiceConnection.class)
		static JdbcServiceConnectionTomcatBeanPostProcessor jdbcServiceConnectionTomcatBeanPostProcessor() {
			return new JdbcServiceConnectionTomcatBeanPostProcessor();
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		org.apache.tomcat.jdbc.pool.DataSource dataSource(DataSourceProperties properties,
				ObjectProvider<JdbcServiceConnection> serviceConnectionProvider) {
			JdbcServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			org.apache.tomcat.jdbc.pool.DataSource dataSource = (serviceConnection != null)
					? createDataSource(serviceConnection, org.apache.tomcat.jdbc.pool.DataSource.class,
							this.classLoader)
					: createDataSource(properties, org.apache.tomcat.jdbc.pool.DataSource.class);
			String validationQuery;
			String url = (serviceConnection != null) ? serviceConnection.getJdbcUrl() : properties.determineUrl();
			DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(url);
			validationQuery = databaseDriver.getValidationQuery();
			if (validationQuery != null) {
				dataSource.setTestOnBorrow(true);
				dataSource.setValidationQuery(validationQuery);
			}
			return dataSource;
		}

	}

	/**
	 * Hikari DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HikariDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource",
			matchIfMissing = true)
	static class Hikari implements BeanClassLoaderAware {

		private ClassLoader classLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Bean
		@ConditionalOnBean(JdbcServiceConnection.class)
		static JdbcServiceConnectionHikariBeanPostProcessor jdbcServiceConnectionHikariBeanPostProcessor() {
			return new JdbcServiceConnectionHikariBeanPostProcessor();
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.hikari")
		HikariDataSource dataSource(DataSourceProperties properties,
				ObjectProvider<JdbcServiceConnection> serviceConnectionProvider) {
			JdbcServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			HikariDataSource dataSource = (serviceConnection != null)
					? createDataSource(serviceConnection, HikariDataSource.class, this.classLoader)
					: createDataSource(properties, HikariDataSource.class);
			if (StringUtils.hasText(properties.getName())) {
				dataSource.setPoolName(properties.getName());
			}
			return dataSource;
		}

	}

	/**
	 * DBCP DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource",
			matchIfMissing = true)
	static class Dbcp2 implements BeanClassLoaderAware {

		private ClassLoader classLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Bean
		@ConditionalOnBean(JdbcServiceConnection.class)
		static JdbcServiceConnectionDbcp2BeanPostProcessor jdbcServiceConnectionDbcp2BeanPostProcessor() {
			return new JdbcServiceConnectionDbcp2BeanPostProcessor();
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.dbcp2")
		org.apache.commons.dbcp2.BasicDataSource dataSource(DataSourceProperties properties,
				ObjectProvider<JdbcServiceConnection> serviceConnectionProvider) {
			JdbcServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			return (serviceConnection != null)
					? createDataSource(serviceConnection, org.apache.commons.dbcp2.BasicDataSource.class,
							this.classLoader)
					: createDataSource(properties, org.apache.commons.dbcp2.BasicDataSource.class);
		}

	}

	/**
	 * Oracle UCP DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "oracle.ucp.jdbc.PoolDataSource",
			matchIfMissing = true)
	static class OracleUcp implements BeanClassLoaderAware {

		private ClassLoader classLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Bean
		@ConditionalOnBean(JdbcServiceConnection.class)
		static JdbcServiceConnectionOracleUcpBeanPostProcessor jdbcServiceConnectionOracleUcpBeanPostProcessor() {
			return new JdbcServiceConnectionOracleUcpBeanPostProcessor();
		}

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.oracleucp")
		PoolDataSourceImpl dataSource(DataSourceProperties properties,
				ObjectProvider<JdbcServiceConnection> serviceConnectionProvider) throws SQLException {
			JdbcServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			PoolDataSourceImpl dataSource = (serviceConnection != null)
					? createDataSource(serviceConnection, PoolDataSourceImpl.class, this.classLoader)
					: createDataSource(properties, PoolDataSourceImpl.class);
			dataSource.setValidateConnectionOnBorrow(true);
			if (StringUtils.hasText(properties.getName())) {
				dataSource.setConnectionPoolName(properties.getName());
			}
			return dataSource;
		}

	}

	/**
	 * Generic DataSource configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(DataSource.class)
	@ConditionalOnProperty(name = "spring.datasource.type")
	static class Generic implements BeanClassLoaderAware {

		private ClassLoader classLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Bean
		DataSource dataSource(DataSourceProperties properties,
				ObjectProvider<JdbcServiceConnection> serviceConnectionProvider) {
			JdbcServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			if (serviceConnection != null) {
				return DataSourceBuilder.create(this.classLoader)
					.url(serviceConnection.getJdbcUrl())
					.username(serviceConnection.getUsername())
					.password(serviceConnection.getPassword())
					.type(properties.getType())
					.build();
			}
			return properties.initializeDataSourceBuilder().build();
		}

	}

}
