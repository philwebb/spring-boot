/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.web.embedded;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ScopeMetadataResolver;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Variant of {@link AnnotationConfigWebApplicationContext} that support
 * {@link EmbeddedServletContainer embedded} servlet containers.
 * @author Phillip Webb
 *
 * @see EmbeddedWebApplicationContext
 */
public class AnnotationConfigEmbeddedWebApplicationContext extends
		EmbeddedWebApplicationContext {

	private Delegate delegate = new Delegate();

	public void register(Class<?>... annotatedClasses) {
		Assert.notEmpty(annotatedClasses,
				"At least one annotated class must be specified");
		this.delegate.register(annotatedClasses);
	}

	public void scan(String... basePackages) {
		this.delegate.scan(basePackages);
	}

	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		this.delegate.loadBeanDefinitions(beanFactory);
	}

	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.delegate.setBeanNameGenerator(beanNameGenerator);
	}

	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.delegate.setScopeMetadataResolver(scopeMetadataResolver);
	}

	private class Delegate extends AnnotationConfigWebApplicationContext {

		@Override
		protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
			super.loadBeanDefinitions(beanFactory);
		}

		@Override
		public ConfigurableEnvironment getEnvironment() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getEnvironment();
		}

		@Override
		public String[] getConfigLocations() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getConfigLocations();
		}

		@Override
		public ClassLoader getClassLoader() {
			return AnnotationConfigEmbeddedWebApplicationContext.this.getClassLoader();
		}
	}

}
