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

package org.springframework.boot.jdbc;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.datasource.OracleDataSource;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.apache.commons.dbcp2.BasicDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 *
 *
 * Convenience class for building a {@link DataSource} with common implementations and
 * properties. If HikariCP, Tomcat, Commons DBCP or Oracle UCP are on the classpath one of
 * them will be selected (in that order with Hikari first). In the interest of a uniform
 * interface, and so that there can be a fallback to an embedded database if one can be
 * detected on the classpath, only a small set of common configuration properties are
 * supported. To inject additional properties into the result you can downcast it, or use
 * {@code @ConfigurationProperties}.
 *
 * @param <T> type of DataSource produced by the builder
 * @author Dave Syer
 * @author Madhura Bhave
 * @author Fabio Grassi
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class DataSourceBuilder<T extends DataSource> {

	private final ClassLoader classLoader;

	private final Map<DataSourceProperty, String> values = new HashMap<>();

	private Class<T> type;

	private DataSourceBuilder(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Set the {@link DataSource} type that should be built.
	 * @param <D> the datasource type
	 * @param type the datasource type
	 * @return this builder
	 */
	@SuppressWarnings("unchecked")
	public <D extends DataSource> DataSourceBuilder<D> type(Class<D> type) {
		this.type = (Class<T>) type;
		return (DataSourceBuilder<D>) this;
	}

	/**
	 * Set the URL that should be used when building the datasource.
	 * @param url the JDBC url
	 * @return this builder
	 */
	public DataSourceBuilder<T> url(String url) {
		set(DataSourceProperty.URL, url);
		return this;
	}

	/**
	 * Set the driver class name that should be used when building the datasource.
	 * @param driverClassName the driver class name
	 * @return this builder
	 */
	public DataSourceBuilder<T> driverClassName(String driverClassName) {
		set(DataSourceProperty.DRIVER_CLASS_NAME, driverClassName);
		return this;
	}

	/**
	 * Set the username that should be used when building the datasource.
	 * @param username the user name
	 * @return this builder
	 */
	public DataSourceBuilder<T> username(String username) {
		set(DataSourceProperty.USERNAME, username);
		return this;
	}

	/**
	 * Set the password that should be used when building the datasource.
	 * @param password the password
	 * @return this builder
	 */
	public DataSourceBuilder<T> password(String password) {
		set(DataSourceProperty.PASSWORD, password);
		return this;
	}

	private void set(DataSourceProperty property, String value) {
		this.values.put(property, value);
	}

	/**
	 * Return a newly built {@link DataSource} instance.
	 * @return the built datasource
	 */
	public T build() {
		Class<T> type = this.type;
		DataSourceProperties<T> properties = DataSourceProperties.forType(this.classLoader, type);
		type = (type != null) ? type : properties.getDataSourceType();
		T dataSource = BeanUtils.instantiateClass(type);
		this.values.forEach((property, value) -> properties.set(dataSource, property, value));
		if (!this.values.containsKey(DataSourceProperty.DRIVER_CLASS_NAME)
				&& properties.canSet(DataSourceProperty.DRIVER_CLASS_NAME)
				&& this.values.containsKey(DataSourceProperty.URL)) {
			String url = this.values.get(DataSourceProperty.URL);
			DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
			properties.set(dataSource, DataSourceProperty.DRIVER_CLASS_NAME, driver.getDriverClassName());
		}
		// FIXME rethrow
		return dataSource;
	}

	/**
	 * Create a new {@link DataSourceBuilder} instance.
	 * @return a new datasource builder instance
	 */
	public static DataSourceBuilder<?> create() {
		return create(null);
	}

	/**
	 * Create a new {@link DataSourceBuilder} instance.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return a new datasource builder instance
	 */
	public static DataSourceBuilder<?> create(ClassLoader classLoader) {
		return new DataSourceBuilder<>(classLoader);
	}

	/**
	 * Find the {@link DataSource} type preferred for the given classloader.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return the preferred datasource type
	 */
	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		MappedDataSourceProperties<?> mappings = MappedDataSourceProperties.forType(classLoader, null);
		return (mappings != null) ? mappings.getDataSourceType() : null;
	}

	/**
	 * An individual DataSource property supported by the builder.
	 */
	private enum DataSourceProperty {

		URL("url"),

		DRIVER_CLASS_NAME("driverClassName"),

		USERNAME("username"),

		PASSWORD("password");

		private final String name;

		DataSourceProperty(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return this.name;
		}

		Method findSetter(Class<?> type) {
			return ReflectionUtils.findMethod(type, "set" + StringUtils.capitalize(this.name), String.class);
		}

		Method findGetter(Class<?> type) {
			return ReflectionUtils.findMethod(type, "get" + StringUtils.capitalize(this.name), String.class);
		}

	}

	private interface DataSourceProperties<T extends DataSource> {

		Class<T> getDataSourceType();

		boolean canSet(DataSourceProperty property);

		void set(T dataSource, DataSourceProperty property, String value);

		static <T extends DataSource> DataSourceProperties<T> forType(ClassLoader classLoader, Class<T> type) {
			MappedDataSourceProperties<T> mapped = MappedDataSourceProperties.forType(classLoader, type);
			return (mapped != null) ? mapped : new ReflectionDataSourceProperties<>(type);
		}

	}

	private static class MappedDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

		private final Map<DataSourceProperty, MappedDataSourceProperty<T, ?>> mappedProperties = new HashMap<>();

		private final Class<T> dataSourceType;

		@SuppressWarnings("unchecked")
		public MappedDataSourceProperties() {
			this.dataSourceType = (Class<T>) ResolvableType.forClass(MappedDataSourceProperties.class, getClass())
					.resolveGeneric();
		}

		@Override
		public Class<T> getDataSourceType() {
			return this.dataSourceType;
		}

		protected void add(DataSourceProperty property, Getter<T, String> getter, Setter<T, String> setter) {
			add(property, String.class, getter, setter);
		}

		protected <V> void add(DataSourceProperty property, Class<V> type, Getter<T, V> getter, Setter<T, V> setter) {
			this.mappedProperties.put(property, new MappedDataSourceProperty<>(property, type, getter, setter));
		}

		@Override
		public boolean canSet(DataSourceProperty property) {
			return this.mappedProperties.containsKey(property);
		}

		@Override
		public void set(T dataSource, DataSourceProperty property, String value) {
			MappedDataSourceProperty<T, ?> mappedProperty = this.mappedProperties.get(property);
			UnsupportedDataSourcePropertyException.throwIf(mappedProperty == null,
					() -> "No mapping found for " + property);
			mappedProperty.set(dataSource, value);
		}

		static <T extends DataSource> MappedDataSourceProperties<T> forType(ClassLoader classLoader, Class<T> type) {
			MappedDataSourceProperties<T> pooled = lookupPooled(classLoader, type);
			if (type == null || pooled != null) {
				return pooled;
			}
			return lookupBasic(classLoader, type);
		}

		private static <T extends DataSource> MappedDataSourceProperties<T> lookupPooled(ClassLoader classLoader,
				Class<T> type) {
			MappedDataSourceProperties<T> result = null;
			result = lookup(classLoader, type, result, "com.zaxxer.hikari.HikariDataSource",
					HikariDataSourceProperties::new);
			result = lookup(classLoader, type, result, "org.apache.tomcat.jdbc.pool.DataSource",
					TomcatPoolDataSourceProperties::new);
			result = lookup(classLoader, type, result, "org.apache.commons.dbcp2.BasicDataSource",
					MappedDbcp2DataSource::new);
			result = lookup(classLoader, type, result, "oracle.ucp.jdbc.PoolDataSourceImpl",
					OraclePoolDataSourceProperties::new, "oracle.jdbc.OracleConnection");
			return result;
		}

		private static <T extends DataSource> MappedDataSourceProperties<T> lookupBasic(ClassLoader classLoader,
				Class<T> dataSourceType) {
			MappedDataSourceProperties<T> result = null;
			result = lookup(classLoader, dataSourceType, result,
					"org.springframework.jdbc.datasource.SimpleDriverDataSource",
					() -> new SimpleDataSourceProperties());
			result = lookup(classLoader, dataSourceType, result, "oracle.jdbc.datasource.OracleDataSource",
					OracleDataSourceProperties::new);
			result = lookup(classLoader, dataSourceType, result, "org.h2.jdbcx.JdbcDataSource",
					H2DataSourceProperties::new);
			result = lookup(classLoader, dataSourceType, result, "org.postgresql.ds.PGSimpleDataSource",
					PostgresDataSourceProperties::new);
			return result;
		}

		@SuppressWarnings("unchecked")
		private static <T extends DataSource> MappedDataSourceProperties<T> lookup(ClassLoader classLoader,
				Class<T> dataSourceType, MappedDataSourceProperties<T> existing, String dataSourceClassName,
				Supplier<MappedDataSourceProperties<?>> propertyMappingsSupplier, String... requiredClassNames) {
			if (existing != null || !allPresent(classLoader, dataSourceClassName, requiredClassNames)) {
				return existing;
			}
			MappedDataSourceProperties<?> propertyMappings = propertyMappingsSupplier.get();
			return (dataSourceType == null || propertyMappings.getDataSourceType().isAssignableFrom(dataSourceType))
					? (MappedDataSourceProperties<T>) propertyMappings : null;
		}

		private static boolean allPresent(ClassLoader classLoader, String dataSourceClassName,
				String[] requiredClassNames) {
			boolean result = ClassUtils.isPresent(dataSourceClassName, classLoader);
			for (String requiredClassName : requiredClassNames) {
				result = result && ClassUtils.isPresent(requiredClassName, classLoader);
			}
			return result;
		}

	}

	private static class MappedDataSourceProperty<T extends DataSource, V> {

		private final DataSourceProperty property;

		private final Class<V> type;

		private final Getter<T, V> getter;

		private final Setter<T, V> setter;

		MappedDataSourceProperty(DataSourceProperty property, Class<V> type, Getter<T, V> getter, Setter<T, V> setter) {
			this.property = property;
			this.type = type;
			this.getter = getter;
			this.setter = setter;
		}

		void set(T dataSource, String value) {
			try {
				UnsupportedDataSourcePropertyException.throwIf(this.setter == null,
						() -> "No setter mapped for " + this.property);
				this.setter.set(dataSource, convertFromString(value));
			}
			catch (SQLException ex) {
				throw new IllegalStateException(ex);
			}
		}

		@SuppressWarnings("unchecked")
		private V convertFromString(String value) {
			if (String.class.equals(this.type)) {
				return (V) value;
			}
			if (Class.class.equals(this.type)) {
				return (V) ClassUtils.resolveClassName(value, null);
			}
			throw new IllegalStateException("Unsupported value type " + this.type);
		}

	}

	private static class ReflectionDataSourceProperties<T extends DataSource> implements DataSourceProperties<T> {

		private final Map<DataSourceProperty, Method> getters;

		private final Map<DataSourceProperty, Method> setters;

		private Class<T> dataSourceType;

		public ReflectionDataSourceProperties(Class<T> dataSourceType) {
			Assert.state(dataSourceType != null, "No supported DataSource type found");
			Map<DataSourceProperty, Method> getters = new HashMap<>();
			Map<DataSourceProperty, Method> setters = new HashMap<>();
			for (DataSourceProperty property : DataSourceProperty.values()) {
				putIfNotNull(getters, property, property.findGetter(dataSourceType));
				putIfNotNull(setters, property, property.findSetter(dataSourceType));
			}
			this.dataSourceType = dataSourceType;
			this.getters = Collections.unmodifiableMap(getters);
			this.setters = Collections.unmodifiableMap(setters);
		}

		private void putIfNotNull(Map<DataSourceProperty, Method> map, DataSourceProperty property, Method method) {
			if (method != null) {
				map.put(property, method);
			}
		}

		@Override
		public Class<T> getDataSourceType() {
			return this.dataSourceType;
		}

		@Override
		public boolean canSet(DataSourceProperty property) {
			return this.setters.containsKey(property);
		}

		@Override
		public void set(T dataSource, DataSourceProperty property, String value) {
			Method method = this.setters.get(property);
			UnsupportedDataSourcePropertyException.throwIf(method == null,
					() -> "Unable to find sutable method for " + property);
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, dataSource, value);
		}

	}

	@FunctionalInterface
	private interface Getter<T, V> {

		V get(T instance) throws SQLException;

	}

	@FunctionalInterface
	private interface Setter<T, V> {

		void set(T instance, V value) throws SQLException;

	}

	/**
	 * {@link MappedDataSource} for Hikari.
	 */
	private static class HikariDataSourceProperties extends MappedDataSourceProperties<HikariDataSource> {

		HikariDataSourceProperties() {
			add(DataSourceProperty.URL, HikariDataSource::getJdbcUrl, HikariDataSource::setJdbcUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, HikariDataSource::getDriverClassName,
					HikariDataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, HikariDataSource::getUsername, HikariDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, HikariDataSource::getPassword, HikariDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Tomcat Pool.
	 */
	private static class TomcatPoolDataSourceProperties
			extends MappedDataSourceProperties<org.apache.tomcat.jdbc.pool.DataSource> {

		TomcatPoolDataSourceProperties() {
			add(DataSourceProperty.URL, org.apache.tomcat.jdbc.pool.DataSource::getUrl,
					org.apache.tomcat.jdbc.pool.DataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, org.apache.tomcat.jdbc.pool.DataSource::getDriverClassName,
					org.apache.tomcat.jdbc.pool.DataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, org.apache.tomcat.jdbc.pool.DataSource::getUsername,
					org.apache.tomcat.jdbc.pool.DataSource::setUsername);
			add(DataSourceProperty.PASSWORD, org.apache.tomcat.jdbc.pool.DataSource::getPassword,
					org.apache.tomcat.jdbc.pool.DataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for DBCP2.
	 */
	private static class MappedDbcp2DataSource extends MappedDataSourceProperties<BasicDataSource> {

		MappedDbcp2DataSource() {
			add(DataSourceProperty.URL, BasicDataSource::getUrl, BasicDataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, BasicDataSource::getDriverClassName,
					BasicDataSource::setDriverClassName);
			add(DataSourceProperty.USERNAME, BasicDataSource::getUsername, BasicDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, BasicDataSource::getPassword, BasicDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Oracle Pool.
	 */
	private static class OraclePoolDataSourceProperties extends MappedDataSourceProperties<PoolDataSourceImpl> {

		OraclePoolDataSourceProperties() {
			add(DataSourceProperty.URL, PoolDataSourceImpl::getURL, PoolDataSourceImpl::setURL);
			add(DataSourceProperty.DRIVER_CLASS_NAME, PoolDataSourceImpl::getConnectionFactoryClassName,
					PoolDataSourceImpl::setConnectionFactoryClassName);
			add(DataSourceProperty.USERNAME, PoolDataSourceImpl::getUser, PoolDataSourceImpl::setUser);
			add(DataSourceProperty.PASSWORD, PoolDataSourceImpl::getPassword, PoolDataSourceImpl::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Spring's {@link SimpleDriverDataSource}.
	 */
	private static class SimpleDataSourceProperties extends MappedDataSourceProperties<SimpleDriverDataSource> {

		SimpleDataSourceProperties() {
			add(DataSourceProperty.URL, SimpleDriverDataSource::getUrl, SimpleDriverDataSource::setUrl);
			add(DataSourceProperty.DRIVER_CLASS_NAME, Class.class, (dataSource) -> dataSource.getDriver().getClass(),
					SimpleDriverDataSource::setDriverClass);
			add(DataSourceProperty.USERNAME, SimpleDriverDataSource::getUsername, SimpleDriverDataSource::setUsername);
			add(DataSourceProperty.PASSWORD, SimpleDriverDataSource::getPassword, SimpleDriverDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Oracle.
	 */
	private static class OracleDataSourceProperties extends MappedDataSourceProperties<OracleDataSource> {

		OracleDataSourceProperties() {
			add(DataSourceProperty.URL, OracleDataSource::getURL, OracleDataSource::setURL);
			add(DataSourceProperty.USERNAME, OracleDataSource::getUser, OracleDataSource::setUser);
			add(DataSourceProperty.PASSWORD, null, OracleDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for H2.
	 */
	private static class H2DataSourceProperties extends MappedDataSourceProperties<JdbcDataSource> {

		H2DataSourceProperties() {
			add(DataSourceProperty.URL, JdbcDataSource::getUrl, JdbcDataSource::setUrl);
			add(DataSourceProperty.USERNAME, JdbcDataSource::getUser, JdbcDataSource::setUser);
			add(DataSourceProperty.PASSWORD, JdbcDataSource::getPassword, JdbcDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Postgres.
	 */
	private static class PostgresDataSourceProperties extends MappedDataSourceProperties<PGSimpleDataSource> {

		PostgresDataSourceProperties() {
			add(DataSourceProperty.URL, PGSimpleDataSource::getUrl, PGSimpleDataSource::setUrl);
			add(DataSourceProperty.USERNAME, PGSimpleDataSource::getUser, PGSimpleDataSource::setUser);
			add(DataSourceProperty.PASSWORD, PGSimpleDataSource::getPassword, PGSimpleDataSource::setPassword);
		}

	}

}
