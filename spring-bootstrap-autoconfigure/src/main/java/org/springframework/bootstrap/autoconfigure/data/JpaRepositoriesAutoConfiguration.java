package org.springframework.bootstrap.autoconfigure.data;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;


@AutoConfiguration
@ConditionalOnClass(JpaRepository.class)
@ConditionalOnMissingBean(JpaRepositoryFactoryBean.class)
@Import(JpaRepositoriesAutoConfigureRegistrar.class)
public class JpaRepositoriesAutoConfiguration {
}
