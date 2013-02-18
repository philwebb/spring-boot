
package org.springframework.bootstrapsample;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
public class SampleApplicationDataConfiguration implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Bean
	public PlatformTransactionManager txManager() {
		return new JpaTransactionManager(entityManagerFactory().getObject());
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setDataSource(dataSource());
		entityManagerFactoryBean.setPackagesToScan("org.springframework.bootstrapsample.domain");
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
		Map<String, Object> properties = entityManagerFactoryBean.getJpaPropertyMap();
		properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		properties.put("hibernate.hbm2ddl.auto", "create-drop");
		properties.put("hibernate.show_sql", "true");
		properties.put("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider");
		return entityManagerFactoryBean;
	}

//	@Bean
//	public DataSource dataSource() {
//		return new EmbeddedDatabaseBuilder().build();
//	}

	private DataSource dataSource() {
		BeanDefinition beanDefinition = ((ConfigurableApplicationContext)applicationContext).getBeanFactory().getBeanDefinition("dataSource");
		beanDefinition.getAttribute("test");
		return applicationContext.getBean(DataSource.class);
	}

	@Bean
	public JpaVendorAdapter jpaVendorAdapter() {
		return new HibernateJpaVendorAdapter();
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
