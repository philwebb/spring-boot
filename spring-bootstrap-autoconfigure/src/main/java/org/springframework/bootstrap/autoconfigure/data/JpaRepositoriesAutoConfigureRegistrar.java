
package org.springframework.bootstrap.autoconfigure.data;

import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.bootstrap.autoconfigure.AutoConfigurationSettings;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryBeanDefinitionBuilder;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

class JpaRepositoriesAutoConfigureRegistrar implements ImportBeanDefinitionRegistrar,
		BeanFactoryAware {

	private BeanFactory beanFactory;

	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
			BeanDefinitionRegistry registry) {

		ResourceLoader resourceLoader = new DefaultResourceLoader();
		AnnotationRepositoryConfigurationSource configurationSource = getConfigurationSource();
		RepositoryConfigurationExtension extension = new JpaRepositoryConfigExtension();
		extension.registerBeansForRoot(registry, configurationSource);

		for (RepositoryConfiguration<AnnotationRepositoryConfigurationSource> repositoryConfiguration : extension.getRepositoryConfigurations(
				configurationSource, resourceLoader)) {
			RepositoryBeanDefinitionBuilder builder = new RepositoryBeanDefinitionBuilder(
					repositoryConfiguration, extension);
			BeanDefinitionBuilder definitionBuilder = builder.build(registry,
					resourceLoader);
			extension.postProcess(definitionBuilder, configurationSource);
			registry.registerBeanDefinition(repositoryConfiguration.getBeanId(),
					definitionBuilder.getBeanDefinition());
		}

	}

	private AnnotationRepositoryConfigurationSource getConfigurationSource() {
		StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(
				EnableJpaRepositoriesConfiguration.class, true);
		AnnotationRepositoryConfigurationSource configurationSource = new AnnotationRepositoryConfigurationSource(
				metadata, EnableJpaRepositories.class) {

			public java.lang.Iterable<String> getBasePackages() {
				return Collections.singleton(getPackageToScan());
			};
		};
		return configurationSource;
	}

	protected String getPackageToScan() {
		return this.beanFactory.getBean(AutoConfigurationSettings.class).getRepositoryPackage();
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@EnableJpaRepositories
	private static class EnableJpaRepositoriesConfiguration {
	}
}
