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

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
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

	private final MappedDataSources mapped;

	private Class<T> type;

	private DataSourceProperties properties;

	private DataSourceBuilder(ClassLoader classLoader) {
		this.mapped = new MappedDataSources(classLoader);
	}

	/**
	 * Set the {@link DataSource} type that should be built.
	 * @param <D> the datasource type
	 * @param type the datasource type
	 * @return this builder
	 */
	@SuppressWarnings("unchecked")
	public <D extends DataSource> DataSourceBuilder<D> type(Class<D> type) {
		return (DataSourceBuilder<D>) this;
	}

	/**
	 * Set the URL that should be used when building the datasource.
	 * @param url the JDBC url
	 * @return this builder
	 */
	public DataSourceBuilder<T> url(String url) {
		this.properties.set(DataSourceProperty.URL, url);
		return this;
	}

	/**
	 * Set the driver class name that should be used when building the datasource.
	 * @param driverClassName the driver class name
	 * @return this builder
	 */
	public DataSourceBuilder<T> driverClassName(String driverClassName) {
		this.properties.set(DataSourceProperty.DRIVER_CLASS_NAME, driverClassName);
		return this;
	}

	/**
	 * Set the username that should be used when building the datasource.
	 * @param username the user name
	 * @return this builder
	 */
	public DataSourceBuilder<T> username(String username) {
		this.properties.set(DataSourceProperty.USERNAME, username);
		return this;
	}

	/**
	 * Set the password that should be used when building the datasource.
	 * @param password the password
	 * @return this builder
	 */
	public DataSourceBuilder<T> password(String password) {
		this.properties.set(DataSourceProperty.PASSWORD, password);
		return this;
	}

	/**
	 * Return a newly built {@link DataSource} instance.
	 * @return the built datasource
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		Class<? extends DataSource> type = (this.type != null) ? this.type : this.mapped.getPreferredType();
		Assert.state(type != null, "No supported DataSource type found");
		DataSource dataSource = BeanUtils.instantiateClass(type);
		DataSourceProperties properties = this.mapped.get(dataSource);
		if (properties == null) {
			properties = new ReflectionDataSourceProperties(dataSource);
		}
		copyProperties(this.properties, properties);
		if (properties.canSet(DataSourceProperty.DRIVER_CLASS_NAME)
				&& !this.properties.isSet(DataSourceProperty.DRIVER_CLASS_NAME)
				&& this.properties.isSet(DataSourceProperty.URL)) {
			String url = this.properties.get(DataSourceProperty.URL);
			properties.set(DataSourceProperty.DRIVER_CLASS_NAME, DatabaseDriver.fromJdbcUrl(url).getDriverClassName());
		}
		return (T) dataSource;
	}

	private void copyProperties(DataSourceProperties source, DataSourceProperties destination) {
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
		return new MappedDataSources(classLoader).getPreferredType();
	}

	/**
	 * An individual DataSource property supported by the builder.
	 */
	private enum DataSourceProperty {

		URL,

		DRIVER_CLASS_NAME,

		USERNAME,

		PASSWORD

	}

	/**
	 * Interface used to access DataSource properties.
	 */
	private interface DataSourceProperties {

		boolean canGet(DataSourceProperty property);

		String get(DataSourceProperty property);

		boolean canSet(DataSourceProperty property);

		void set(DataSourceProperty property, String value);

		default boolean isSet(DataSourceProperty property) {
			return StringUtils.hasText(get(property));
		}

	}

	/**
	 * Provides access to {@link MappedDataSource} instances.
	 */
	private static class MappedDataSources {

		private final Map<Class<? extends DataSource>, Function<DataSource, MappedDataSource<?>>> mapped;

		private final Class<? extends DataSource> preferredType;

		public MappedDataSources(ClassLoader classLoader) {
			Map<Class<? extends DataSource>, Function<DataSource, MappedDataSource<?>>> pooled = new LinkedHashMap<>();
			BiFunction<ClassLoader, DataSource, MappedDataSource<?>> biFunction = MappedHikariDataSource::new;
			putIfPresent(pooled, classLoader, () -> MappedHikariDataSource.class, biFunction);
			putIfPresent(pooled, classLoader, () -> MappedTomcatPoolDataSource.class, MappedTomcatPoolDataSource::new);
			putIfPresent(pooled, classLoader, () -> MappedDbcp2DataSource.class, MappedDbcp2DataSource::new);
			putIfPresent(pooled, classLoader, () -> MappedOraclePoolDataSource.class, MappedOraclePoolDataSource::new,
					"oracle.jdbc.OracleConnection");
			Map<Class<? extends DataSource>, Function<DataSource, MappedDataSource<?>>> all = new LinkedHashMap<>();
			all.putAll(pooled);
			putIfPresent(all, classLoader, () -> MappedSimpleDataSource.class, MappedSimpleDataSource::new);
			putIfPresent(all, classLoader, () -> MappedOracleDataSource.class, MappedOracleDataSource::new);
			putIfPresent(all, classLoader, () -> MappedH2DataSource.class, MappedH2DataSource::new);
			putIfPresent(all, classLoader, () -> MappedPostgresDataSource.class, MappedPostgresDataSource::new);
			this.preferredType = CollectionUtils.firstElement(pooled.keySet());
			this.mapped = Collections.unmodifiableMap(pooled);
		}

		@SuppressWarnings("unchecked")
		private <D extends DataSource, M extends MappedDataSource<D>> void putIfPresent(
				Map<Class<? extends DataSource>, Function<DataSource, MappedDataSource<?>>> map,
				ClassLoader classLoader, Supplier<Class<M>> mappedDataSourceType,
				BiFunction<ClassLoader, D, M> constructor, String... requiredClassNames) {
			for (String requiredClassName : requiredClassNames) {
				if (!ClassUtils.isPresent(requiredClassName, classLoader)) {
					return;
				}
			}
			Class<? extends DataSource> dataSourceType = getDataSourceClass(mappedDataSourceType);
			if (dataSourceType != null) {
				map.put(dataSourceType, (datasource) -> constructor.apply(classLoader, (D) datasource));
			}
		}

		@SuppressWarnings("unchecked")
		private <M extends MappedDataSource<?>> Class<? extends DataSource> getDataSourceClass(
				Supplier<Class<M>> mappedDataSourceType) {
			try {
				return (Class<? extends DataSource>) ResolvableType
						.forClass(MappedDataSource.class, mappedDataSourceType.get()).resolveGeneric();
			}
			catch (NoClassDefFoundError ex) {
				return null;
			}
		}

		DataSourceProperties get(DataSource dataSource) {
			return null;
		}

		Class<? extends DataSource> getPreferredType() {
			return this.preferredType;
		}

	}

	/**
	 * Base class for {@link DataSourceProperties} mapped to a supported type.
	 */
	private static abstract class MappedDataSource<T extends DataSource> implements DataSourceProperties {

		private final ClassLoader classLoader;

		private final T dataSource;

		private final Mappings mappings;

		MappedDataSource(ClassLoader classLoader, T dataSource) {
			this.classLoader = classLoader;
			this.dataSource = dataSource;
			this.mappings = new Mappings();
			configure(this.mappings);
		}

		protected abstract void configure(Mappings mappings);

		@Override
		public boolean canGet(DataSourceProperty property) {
			Mapped mapped = this.mappings.get(property);
			return (mapped != null && mapped.canGet());
		}

		@Override
		public String get(DataSourceProperty property) {
			Mapped mapped = this.mappings.get(property);
			// FIXME assert?
			return mapped.get(property);
		}

		@Override
		public boolean canSet(DataSourceProperty property) {
			Mapped mapped = this.mappings.get(property);
			return (mapped != null && mapped.canSet());
		}

		@Override
		public void set(DataSourceProperty property, String value) {
			Mapped mapped = this.mappings.get(property);
			// FIXME assert?
			mapped.set(value);
		}

		class Mappings {

			private final Map<DataSourceProperty, Mapped> mappings = new LinkedHashMap<>();

			void add(DataSourceProperty property, Getter<T, String> getter, Setter<T, String> setter) {
				add(property, String.class, getter, setter);
			}

			<V> void add(DataSourceProperty property, Class<V> valueType, Getter<T, V> getter, Setter<T, V> setter) {
				this.mappings.put(property, new Mapped(valueType, getter, setter));
			}

			MappedDataSource<T>.Mapped get(DataSourceProperty property) {
				return this.mappings.get(property);
			}

		}

		private class Mapped {

			private final Getter<T, String> getter;

			private final Setter<T, String> setter;

			@SuppressWarnings("unchecked")
			public <V> Mapped(Class<V> valueType, Getter<T, V> getter, Setter<T, V> setter) {
				if (String.class.equals(valueType)) {
					this.getter = (Getter<T, String>) getter;
					this.setter = (Setter<T, String>) setter;
				}
				else if (Class.class.equals(valueType)) {
					this.getter = adaptClassGetter((Getter<T, Class<?>>) getter);
					this.setter = adaptClassSetter((Setter<T, Class<?>>) setter);
				}
				else {
					throw new IllegalStateException("Unsupported value type " + valueType);
				}
			}

			private <V> Getter<T, String> adaptClassGetter(Getter<T, Class<?>> getter) {
				if (getter == null) {
					return null;
				}
				return (instance) -> getClassName(getter.get(instance));
			}

			private <V> Setter<T, String> adaptClassSetter(Setter<T, Class<?>> setter) {
				if (setter == null) {
					return null;
				}
				return (instance, value) -> setter.set(instance,
						ClassUtils.resolveClassName(value, MappedDataSource.this.classLoader));
			}

			private String getClassName(Class<?> type) {
				return (type != null) ? type.getName() : null;
			}

			public boolean canGet() {
				return this.getter != null;
			}

			public String get(DataSourceProperty property) {
				try {
					return this.getter.get(MappedDataSource.this.dataSource);
				}
				catch (SQLException ex) {
					throw new IllegalStateException(ex);
				}
			}

			public boolean canSet() {
				return this.setter != null;
			}

			public void set(String value) {
				try {
					this.setter.set(MappedDataSource.this.dataSource, value);
				}
				catch (SQLException ex) {
					throw new IllegalStateException(ex);
				}
			}

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
	private static class MappedHikariDataSource extends MappedDataSource<HikariDataSource> {

		MappedHikariDataSource(ClassLoader classLoader, HikariDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, HikariDataSource::getJdbcUrl, HikariDataSource::setJdbcUrl);
			mappings.add(DataSourceProperty.DRIVER_CLASS_NAME, HikariDataSource::getDriverClassName,
					HikariDataSource::setDriverClassName);
			mappings.add(DataSourceProperty.USERNAME, HikariDataSource::getUsername, HikariDataSource::setUsername);
			mappings.add(DataSourceProperty.PASSWORD, HikariDataSource::getPassword, HikariDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Tomcat Pool.
	 */
	private static class MappedTomcatPoolDataSource extends MappedDataSource<org.apache.tomcat.jdbc.pool.DataSource> {

		MappedTomcatPoolDataSource(ClassLoader classLoader, org.apache.tomcat.jdbc.pool.DataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, org.apache.tomcat.jdbc.pool.DataSource::getUrl,
					org.apache.tomcat.jdbc.pool.DataSource::setUrl);
			mappings.add(DataSourceProperty.DRIVER_CLASS_NAME,
					org.apache.tomcat.jdbc.pool.DataSource::getDriverClassName,
					org.apache.tomcat.jdbc.pool.DataSource::setDriverClassName);
			mappings.add(DataSourceProperty.USERNAME, org.apache.tomcat.jdbc.pool.DataSource::getUsername,
					org.apache.tomcat.jdbc.pool.DataSource::setUsername);
			mappings.add(DataSourceProperty.PASSWORD, org.apache.tomcat.jdbc.pool.DataSource::getPassword,
					org.apache.tomcat.jdbc.pool.DataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for DBCP2.
	 */
	private static class MappedDbcp2DataSource extends MappedDataSource<BasicDataSource> {

		MappedDbcp2DataSource(ClassLoader classLoader, BasicDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, BasicDataSource::getUrl, BasicDataSource::setUrl);
			mappings.add(DataSourceProperty.DRIVER_CLASS_NAME, BasicDataSource::getDriverClassName,
					BasicDataSource::setDriverClassName);
			mappings.add(DataSourceProperty.USERNAME, BasicDataSource::getUsername, BasicDataSource::setUsername);
			mappings.add(DataSourceProperty.PASSWORD, BasicDataSource::getPassword, BasicDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Oracle Pool.
	 */
	private static class MappedOraclePoolDataSource extends MappedDataSource<PoolDataSourceImpl> {

		MappedOraclePoolDataSource(ClassLoader classLoader, PoolDataSourceImpl dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, PoolDataSourceImpl::getURL, PoolDataSourceImpl::setURL);
			mappings.add(DataSourceProperty.DRIVER_CLASS_NAME, PoolDataSourceImpl::getConnectionFactoryClassName,
					PoolDataSourceImpl::setConnectionFactoryClassName);
			mappings.add(DataSourceProperty.USERNAME, PoolDataSourceImpl::getUser, PoolDataSourceImpl::setUser);
			mappings.add(DataSourceProperty.PASSWORD, PoolDataSourceImpl::getPassword, PoolDataSourceImpl::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Spring's {@link SimpleDriverDataSource}.
	 */
	private static class MappedSimpleDataSource extends MappedDataSource<SimpleDriverDataSource> {

		MappedSimpleDataSource(ClassLoader classLoader, SimpleDriverDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, SimpleDriverDataSource::getUrl, SimpleDriverDataSource::setUrl);
			mappings.add(DataSourceProperty.DRIVER_CLASS_NAME, Class.class,
					(dataSource) -> dataSource.getDriver().getClass(), SimpleDriverDataSource::setDriverClass);
			mappings.add(DataSourceProperty.USERNAME, SimpleDriverDataSource::getUsername,
					SimpleDriverDataSource::setUsername);
			mappings.add(DataSourceProperty.PASSWORD, SimpleDriverDataSource::getPassword,
					SimpleDriverDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Oracle.
	 */
	private static class MappedOracleDataSource extends MappedDataSource<OracleDataSource> {

		MappedOracleDataSource(ClassLoader classLoader, OracleDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, OracleDataSource::getURL, OracleDataSource::setURL);
			mappings.add(DataSourceProperty.USERNAME, OracleDataSource::getUser, OracleDataSource::setUser);
			mappings.add(DataSourceProperty.PASSWORD, null, OracleDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for H2.
	 */
	private static class MappedH2DataSource extends MappedDataSource<JdbcDataSource> {

		MappedH2DataSource(ClassLoader classLoader, JdbcDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, JdbcDataSource::getUrl, JdbcDataSource::setUrl);
			mappings.add(DataSourceProperty.USERNAME, JdbcDataSource::getUser, JdbcDataSource::setUser);
			mappings.add(DataSourceProperty.PASSWORD, JdbcDataSource::getPassword, JdbcDataSource::setPassword);
		}

	}

	/**
	 * {@link MappedDataSource} for Postgres.
	 */
	private static class MappedPostgresDataSource extends MappedDataSource<PGSimpleDataSource> {

		MappedPostgresDataSource(ClassLoader classLoader, PGSimpleDataSource dataSource) {
			super(classLoader, dataSource);
		}

		@Override
		protected void configure(Mappings mappings) {
			mappings.add(DataSourceProperty.URL, PGSimpleDataSource::getUrl, PGSimpleDataSource::setUrl);
			mappings.add(DataSourceProperty.USERNAME, PGSimpleDataSource::getUser, PGSimpleDataSource::setUser);
			mappings.add(DataSourceProperty.PASSWORD, PGSimpleDataSource::getPassword, PGSimpleDataSource::setPassword);
		}

	}

	private static class ReflectionDataSourceProperties implements DataSourceProperties {

		/**
		 * @param dataSource
		 */
		public ReflectionDataSourceProperties(DataSource dataSource) {
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean canGet(DataSourceProperty property) {
			return false;
		}

		@Override
		public String get(DataSourceProperty property) {
			return null;
		}

		@Override
		public boolean canSet(DataSourceProperty property) {
			return false;
		}

		@Override
		public void set(DataSourceProperty property, String value) {
		}

	}

	/**
	 * In-memory {@link DataSourceProperties}.
	 */
	private static class InMemoryDataSourceProperties implements DataSourceProperties {

		private final Map<DataSourceProperty, String> values = new LinkedHashMap<>();

		@Override
		public boolean canGet(DataSourceProperty property) {
			return true;
		}

		@Override
		public String get(DataSourceProperty property) {
			return this.values.get(property);
		}

		@Override
		public boolean canSet(DataSourceProperty property) {
			return true;
		}

		@Override
		public void set(DataSourceProperty property, String value) {
			this.values.put(property, value);
		}

	}

}
