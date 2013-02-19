package org.springframework.bootstrap.autoconfigure.orm.jpa;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.jdbc.EmbeddedDatabaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@AutoConfiguration
@ConditionalOnClass(name="org.hibernate.ejb.HibernateEntityManager")
@EnableTransactionManagement
public class HibernateJpaAutoConfiguration extends JpaAutoConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_DIALECTS;
	static {
		EMBEDDED_DATABASE_DIALECTS = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_DIALECTS.put(EmbeddedDatabaseType.HSQL, "org.hibernate.dialect.HSQLDialect");
		// FIXME additional types
	}


	@Bean
	@Override
	public JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	@Override
	protected void configure(
			LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
		Map<String, Object> properties = entityManagerFactoryBean.getJpaPropertyMap();
		if(isAutoConfiguredDataSource()) {
			properties.put("hibernate.hbm2ddl.auto", "create-drop");
			String dialect = EMBEDDED_DATABASE_DIALECTS.get(EmbeddedDatabaseAutoConfiguration.getEmbeddedDatabaseType());
			if(dialect != null) {
				properties.put("hibernate.dialect", dialect);
			}
		}
		properties.put("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider");
	}

}
