/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.orm.jpa.hibernate;

import javax.persistence.Table;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaMetadataProvider;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.core.env.StandardEnvironment;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link PropertyPlaceholderHibernateConfigurationPostProcessor}.
 * 
 * @author Phillip Webb
 */
public class PropertyPlaceholderHibernateConfigurationPostProcessorTests {

	@Test
	public void resolveTableSchema() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		EnvironmentTestUtils.addEnvironment(environment, "a=b");

		PropertyPlaceholderHibernateConfigurationPostProcessor postProcessor = new PropertyPlaceholderHibernateConfigurationPostProcessor();
		postProcessor.addResolvableAttributes(Table.class, "schema");
		postProcessor.setEnvironment(environment);
		MetadataProvider provider = runPostProcessor(postProcessor);

		AnnotationReader reader = provider.getAnnotationReader(Example.class);
		assertThat(reader.getAnnotation(Table.class).schema(), equalTo("b"));
		assertThat(((Table) reader.getAnnotations()[0]).schema(), equalTo("b"));
	}

	private MetadataProvider runPostProcessor(
			PropertyPlaceholderHibernateConfigurationPostProcessor postProcessor) {
		MetadataProvider metadataProvider = new JavaMetadataProvider();
		Configuration configuration = mock(Configuration.class);
		ReflectionManager reflectionManager = mock(ReflectionManager.class,
				withSettings().extraInterfaces(MetadataProviderInjector.class));
		given(configuration.getReflectionManager()).willReturn(reflectionManager);
		given(((MetadataProviderInjector) reflectionManager).getMetadataProvider())
				.willReturn(metadataProvider);

		postProcessor.postProcessConfiguration(configuration);

		ArgumentCaptor<MetadataProvider> metadataProviderCaptor = ArgumentCaptor
				.forClass(MetadataProvider.class);
		verify(((MetadataProviderInjector) reflectionManager)).setMetadataProvider(
				metadataProviderCaptor.capture());
		return metadataProviderCaptor.getValue();
	}

	@Table(schema = "${a}")
	public static class Example {
	}

}
