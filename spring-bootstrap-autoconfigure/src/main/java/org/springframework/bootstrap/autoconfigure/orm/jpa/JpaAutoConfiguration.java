package org.springframework.bootstrap.autoconfigure.orm.jpa;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.bootstrap.autoconfigure.AutoConfigurationSettings;
import org.springframework.bootstrap.autoconfigure.ConditionalOnBean;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.jdbc.EmbeddedDatabaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ConditionalOnClass({LocalContainerEntityManagerFactoryBean.class, EnableTransactionManagement.class, EntityManager.class})
@ConditionalOnBean(DataSource.class)
public abstract class JpaAutoConfiguration implements ApplicationContextAware {

	private ConfigurableApplicationContext applicationContext;

	@Bean
	public PlatformTransactionManager txManager() {
		return new JpaTransactionManager(entityManagerFactory().getObject());
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter());
		entityManagerFactoryBean.setDataSource(getDataSource());
		entityManagerFactoryBean.setPackagesToScan(getPackagesToScan());
		configure(entityManagerFactoryBean);
		return entityManagerFactoryBean;
	}

	protected boolean isAutoConfiguredDataSource() {
		try {
			BeanDefinition beanDefinition = this.applicationContext.getBeanFactory().getBeanDefinition("dataSource");
			return EmbeddedDatabaseAutoConfiguration.class.getName().equals(beanDefinition.getFactoryBeanName());
		} catch(NoSuchBeanDefinitionException e) {
			return false;
		}
	}

	@Bean
	public abstract JpaVendorAdapter jpaVendorAdapter();

	protected DataSource getDataSource() {
		return this.applicationContext.getBean(DataSource.class);
	}

	protected String getPackagesToScan() {
		return this.applicationContext.getBean(AutoConfigurationSettings.class).getDomainPackage();
	}

	protected void configure(LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}
}
