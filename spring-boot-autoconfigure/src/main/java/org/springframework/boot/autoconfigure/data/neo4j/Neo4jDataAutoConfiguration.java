/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j support.
 * <p>
 * Registers a {@link Neo4jTemplate} bean if no other bean of the same type is configured.
 *
 * @author Michael Hunger
 * @author Phillip Webb
 * @since 1.3.0
 */
@Configuration
@EnableConfigurationProperties(Neo4jProperties.class)
@ConditionalOnMissingBean(type = "org.springframework.data.neo4j.template.Neo4jTemplate")
@ConditionalOnClass({ Neo4jServer.class, Neo4jSession.class, Neo4jTemplate.class })
@AutoConfigureAfter({ DataSourceTransactionManagerAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class })
public class Neo4jDataAutoConfiguration implements BeanClassLoaderAware,
		BeanFactoryAware, ResourceLoaderAware {

	@Autowired
	private Neo4jProperties properties;

	@Autowired
	private Environment environment;

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private ResourceLoader resourceLoader;

	@Bean
	@ConditionalOnMissingBean
	public SessionFactory neo4jSessionFactory() {
		Collection<String> packages = getMappingBasePackages(this.beanFactory);
		return new SessionFactory(packages.toArray(new String[packages.size()]));
	}

	@Bean
	@ConditionalOnMissingBean
	public Neo4jServer neo4jServer() {
		return this.properties.createNeo4jServer();
	}

	@Bean
	@ConditionalOnMissingBean
	public Session neo4jSession(SessionFactory sessionFactory, Neo4jServer server) {
		String url = server.url();
		if (server.username() == null && server.password() == null) {
			return sessionFactory.openSession(url);
		}
		return sessionFactory.openSession(url, server.username(), server.password());
	}

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(Session session) throws Exception {
		return new Neo4jTransactionManager(session);
	}

	@Bean
	@ConditionalOnMissingBean
	public Neo4jTemplate neo4jTemplate(Session session) throws Exception {
		return new Neo4jTemplate(session);
	}

	@Bean
	public ConversionService neo4jConversionService(SessionFactory factory) {
		return new MetaDataDrivenConversionService(factory.metaData());
	}

	@Bean
	@ConditionalOnMissingBean
	public Neo4jMappingContext neo4jMappingContext(SessionFactory sessionFactory)
			throws ClassNotFoundException {
		Neo4jMappingContext context = new Neo4jMappingContext(sessionFactory.metaData());
		context.setInitialEntitySet(getInitialEntitySet(this.beanFactory));
		return context;
	}

	private Set<Class<?>> getInitialEntitySet(BeanFactory beanFactory)
			throws ClassNotFoundException {
		Set<Class<?>> entitySet = new HashSet<Class<?>>();
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);
		scanner.setEnvironment(this.environment);
		scanner.setResourceLoader(this.resourceLoader);
		scanner.addIncludeFilter(new AnnotationTypeFilter(NodeEntity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(RelationshipEntity.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));
		for (String basePackage : getMappingBasePackages(beanFactory)) {
			if (StringUtils.hasText(basePackage)) {
				for (BeanDefinition candidate : scanner
						.findCandidateComponents(basePackage)) {
					entitySet.add(ClassUtils.forName(candidate.getBeanClassName(),
							this.classLoader));
				}
			}
		}
		return entitySet;
	}

	private Collection<String> getMappingBasePackages(BeanFactory beanFactory) {
		try {
			return AutoConfigurationPackages.get(beanFactory);
		}
		catch (IllegalStateException ex) {
			// no auto-configuration package registered yet
			return Collections.emptyList();
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
