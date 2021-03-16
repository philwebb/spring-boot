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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.sql.DataSource;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
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
 * @since 2.0.0
 */
public final class DataSourceBuilder2<T extends DataSource> {

	private Class<? extends DataSource> type;

	private final XDataSourceSettingsResolver settingsResolver;

	private final Map<String, String> properties = new HashMap<>();

	private DataSourceBuilder2(ClassLoader classLoader) {
		this.settingsResolver = new XDataSourceSettingsResolver(classLoader);
	}

	/**
	 * Set the {@link DataSource} type that should be built.
	 * @param <D> the datasource type
	 * @param type the datasource type
	 * @return this builder
	 */
	@SuppressWarnings("unchecked")
	public <D extends DataSource> DataSourceBuilder2<D> type(Class<D> type) {
		this.type = type;
		return (DataSourceBuilder2<D>) this;
	}

	/**
	 * Set the URL that should be used when building the datasource.
	 * @param url the JDBC url
	 * @return this builder
	 */
	public DataSourceBuilder2<T> url(String url) {
		this.properties.put("url", url);
		return this;
	}

	/**
	 * Set the driver class name that should be used when building the datasource.
	 * @param driverClassName the driver class name
	 * @return this builder
	 */
	public DataSourceBuilder2<T> driverClassName(String driverClassName) {
		this.properties.put("driverClassName", driverClassName);
		return this;
	}

	/**
	 * Set the username that should be used when building the datasource.
	 * @param username the user name
	 * @return this builder
	 */
	public DataSourceBuilder2<T> username(String username) {
		this.properties.put("username", username);
		return this;
	}

	/**
	 * Set the password that should be used when building the datasource.
	 * @param password the password
	 * @return this builder
	 */
	public DataSourceBuilder2<T> password(String password) {
		this.properties.put("password", password);
		return this;
	}

	/**
	 * Return a newly built {@link DataSource} instance.
	 * @return the built datasource
	 */
	@SuppressWarnings("unchecked")
	public T build() {
		Class<? extends DataSource> type = getType();
		DataSource result = BeanUtils.instantiateClass(type);
		bind(result);
		return (T) result;
	}

	private Class<? extends DataSource> getType() {
		if (this.type != null) {
			return this.type;
		}
		XDataSourceSettings preferred = this.settingsResolver.getPreferred();
		Assert.state(preferred != null, "No supported DataSource type found");
		return preferred.getType();
	}

	private void bind(DataSource result) {
		ConfigurationPropertySource source = getBindSource();
		Binder binder = new Binder(source.withAliases(this.settingsResolver.getAliases(result)));
		binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(result));
	}

	private ConfigurationPropertySource getBindSource() {
		String driverClassName = this.properties.get("driverClassName");
		String url = this.properties.get("url");
		if (StringUtils.hasText(driverClassName) || !StringUtils.hasText(url)) {
			return new MapConfigurationPropertySource(this.properties);
		}
		Map<String, String> properties = new LinkedHashMap<>(this.properties);
		properties.put("driverClassName", DatabaseDriver.fromJdbcUrl(url).getDriverClassName());
		return new MapConfigurationPropertySource(properties);
	}

	/**
	 * Create a new {@link DataSourceBuilder2} instance.
	 * @return a new datasource builder instance
	 */
	public static DataSourceBuilder2<?> create() {
		return new DataSourceBuilder2<>(null);
	}

	/**
	 * Create a new {@link DataSourceBuilder2} instance.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return a new datasource builder instance
	 */
	public static DataSourceBuilder2<?> create(ClassLoader classLoader) {
		return new DataSourceBuilder2<>(classLoader);
	}

	/**
	 * Find the {@link DataSource} type preferred for the given classloader.
	 * @param classLoader the classloader used to discover preferred settings
	 * @return the preferred datasource type
	 */
	public static Class<? extends DataSource> findType(ClassLoader classLoader) {
		XDataSourceSettings preferred = new XDataSourceSettingsResolver(classLoader).getPreferred();
		return (preferred != null) ? preferred.getType() : null;
	}

	private static class XDataSourceSettingsResolver {

		private final XDataSourceSettings preferred;

		private final List<XDataSourceSettings> all;

		XDataSourceSettingsResolver(ClassLoader classLoader) {
			List<XDataSourceSettings> supported = resolveAvailableDataSourceSettings(classLoader);
			this.preferred = (!supported.isEmpty()) ? supported.get(0) : null;
			this.all = new ArrayList<>(supported);
			addIfAvailable(this.all,
					create(classLoader, "org.springframework.jdbc.datasource.SimpleDriverDataSource",
							(type) -> new XDataSourceSettings(type,
									(aliases) -> aliases.addAliases("driver-class-name", "driver-class"))));
			addIfAvailable(this.all,
					create(classLoader, "oracle.jdbc.datasource.OracleDataSource", XOracleDataSourceSettings::new));
			addIfAvailable(this.all, create(classLoader, "org.h2.jdbcx.JdbcDataSource",
					(type) -> new XDataSourceSettings(type, (aliases) -> aliases.addAliases("username", "user"))));
			addIfAvailable(this.all, create(classLoader, "org.postgresql.ds.PGSimpleDataSource",
					(type) -> new XDataSourceSettings(type, (aliases) -> aliases.addAliases("username", "user"))));
		}

		private List<XDataSourceSettings> resolveAvailableDataSourceSettings(ClassLoader classLoader) {
			List<XDataSourceSettings> supported = new ArrayList<>();
			addIfAvailable(supported, create(classLoader, "com.zaxxer.hikari.HikariDataSource",
					(type) -> new XDataSourceSettings(type, (aliases) -> aliases.addAliases("url", "jdbc-url"))));
			addIfAvailable(supported,
					create(classLoader, "org.apache.tomcat.jdbc.pool.DataSource", XDataSourceSettings::new));
			addIfAvailable(supported,
					create(classLoader, "org.apache.commons.dbcp2.BasicDataSource", XDataSourceSettings::new));
			addIfAvailable(supported, create(classLoader, "oracle.ucp.jdbc.PoolDataSourceImpl", (type) -> {
				// Unfortunately Oracle UCP has an import on the Oracle driver itself
				if (ClassUtils.isPresent("oracle.jdbc.OracleConnection", classLoader)) {
					return new XDataSourceSettings(type, (aliases) -> {
						aliases.addAliases("username", "user");
						aliases.addAliases("driver-class-name", "connection-factory-class-name");
					});
				}
				return null;
			}));
			return supported;
		}

		@SuppressWarnings("unchecked")
		private static XDataSourceSettings create(ClassLoader classLoader, String target,
				Function<Class<? extends DataSource>, XDataSourceSettings> factory) {
			if (ClassUtils.isPresent(target, classLoader)) {
				try {
					Class<? extends DataSource> type = (Class<? extends DataSource>) ClassUtils.forName(target,
							classLoader);
					return factory.apply(type);
				}
				catch (Exception ex) {
					// Ignore
				}
			}
			return null;
		}

		private static void addIfAvailable(Collection<XDataSourceSettings> list,
				XDataSourceSettings dataSourceSettings) {
			if (dataSourceSettings != null) {
				list.add(dataSourceSettings);
			}
		}

		ConfigurationPropertyNameAliases getAliases(DataSource result) {
			ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
			this.all.forEach((settings) -> settings.registerAliases(result, aliases));
			return aliases;
		}

		XDataSourceSettings getPreferred() {
			return this.preferred;
		}

	}

	private static class XDataSourceSettings {

		private final Class<? extends DataSource> type;

		private final Consumer<ConfigurationPropertyNameAliases> aliasesCustomizer;

		XDataSourceSettings(Class<? extends DataSource> type,
				Consumer<ConfigurationPropertyNameAliases> aliasesCustomizer) {
			this.type = type;
			this.aliasesCustomizer = aliasesCustomizer;
		}

		XDataSourceSettings(Class<? extends DataSource> type) {
			this(type, (aliases) -> {
			});
		}

		Class<? extends DataSource> getType() {
			return this.type;
		}

		void registerAliases(DataSource candidate, ConfigurationPropertyNameAliases aliases) {
			if (this.type != null && this.type.isInstance(candidate)) {
				this.aliasesCustomizer.accept(aliases);
			}
		}

	}

	private static class XOracleDataSourceSettings extends XDataSourceSettings {

		XOracleDataSourceSettings(Class<? extends DataSource> type) {
			super(type, (aliases) -> aliases.addAliases("username", "user"));
		}

		@Override
		public Class<? extends DataSource> getType() {
			return null; // Base interface
		}

	}

	private static class Dunno<T extends DataSource> {

		Dunno() {
			ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();

		}

		ConfigurationPropertyNameAliases getAliases() {
			return null;
		}

	}

	private static class SimpleDunno extends Dunno<org.springframework.jdbc.datasource.SimpleDriverDataSource> {

		SimpleDunno() {

			bindAliases.addAliases("driver-class-name", "driver-class");
		}

	}

	private static class OracleDunno extends Dunno<oracle.jdbc.datasource.OracleDataSource> {

	}

	private static class H2Dunno extends Dunno<org.h2.jdbcx.JdbcDataSource> {

	}

	private static class PostgressDunno extends Dunno<org.postgresql.ds.PGSimpleDataSource> {

	}

	private static class HikariDunno extends Dunno<com.zaxxer.hikari.HikariDataSource> {

	}

	private static class TomcatDunno extends Dunno<org.apache.tomcat.jdbc.pool.DataSource> {

	}

	private static class DbcpDunno extends Dunno<org.apache.commons.dbcp2.BasicDataSource> {

	}

	private static class Oracle2Dunno extends Dunno<oracle.ucp.jdbc.PoolDataSourceImpl> {

	}

}
