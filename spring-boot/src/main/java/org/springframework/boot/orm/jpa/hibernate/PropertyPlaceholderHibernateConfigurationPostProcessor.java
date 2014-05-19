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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.cfg.Configuration;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * {@link HibernateConfigurationPostProcessor} to resolve property placeholder when
 * loading specific annotations with Hibernate.
 * 
 * @author Phillip Webb
 * @since 1.1.0
 * @see #addResolvableAttributes(Class, String...)
 */
public class PropertyPlaceholderHibernateConfigurationPostProcessor implements
		HibernateConfigurationPostProcessor, EnvironmentAware {

	private Map<Class<?>, ResolvableAnnotation> resolvableAnnotations = new HashMap<Class<?>, ResolvableAnnotation>();

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Add details of an annotation attribute that should be resolved against the Spring
	 * {@link Environment}.
	 * @param annotationType the annotation type
	 * @param attributes the attributes that should be resolved
	 */
	public void addResolvableAttributes(Class<? extends Annotation> annotationType,
			String... attributes) {
		Assert.notNull(annotationType, "AnnotationType must not be null");
		ResolvableAnnotation resolvableAnnotation = this.resolvableAnnotations
				.get(annotationType);
		if (resolvableAnnotation == null) {
			resolvableAnnotation = new ResolvableAnnotation(annotationType);
			this.resolvableAnnotations.put(annotationType, resolvableAnnotation);
		}
		resolvableAnnotation.addResolvableAttributes(attributes);
	}

	@Override
	public void postProcessConfiguration(Configuration configuration) {
		ReflectionManager manager = configuration.getReflectionManager();
		MetadataProviderInjector injector = (MetadataProviderInjector) manager;
		MetadataProvider provider = injector.getMetadataProvider();
		MetadataProvider wrapped = new ResolvableMetadataProvider(provider);
		injector.setMetadataProvider(wrapped);
	}

	/**
	 * An annotation with one or more attributes that should be resolved against the
	 * Spring {@link Environment}.
	 */
	private class ResolvableAnnotation {

		private final Class<? extends Annotation> type;

		private final Set<String> resolvableAttributes = new HashSet<String>();

		public ResolvableAnnotation(Class<? extends Annotation> type) {
			this.type = type;
		}

		public void addResolvableAttributes(String... attributes) {
			this.resolvableAttributes.addAll(Arrays.asList(attributes));
		}

		public <T extends Annotation> T getResolvableVersion(T annotation) {
			AnnotationDescriptor descriptor = createAnnotationDescriptor(annotation);
			return AnnotationFactory.create(descriptor);
		}

		public AnnotationDescriptor createAnnotationDescriptor(Annotation annotation) {
			AnnotationDescriptor descriptor = new AnnotationDescriptor(this.type);
			for (Method method : this.type.getDeclaredMethods()) {
				String name = method.getName();
				Object value = resolveValue(annotation, method);
				descriptor.setValue(name, value);
			}
			return descriptor;
		}

		private Object resolveValue(Annotation annotation, Method method) {
			try {
				Object value = method.invoke(annotation);
				if (value != null && this.resolvableAttributes.contains(method.getName())) {
					return PropertyPlaceholderHibernateConfigurationPostProcessor.this.environment
							.resolvePlaceholders((String) value);
				}
				return value;
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to resolve "
						+ annotation.getClass() + "." + method.getName(), ex);
			}
		}
	}

	/**
	 * {@link MetadataProvider} decorator to also resolve environment place holders.
	 */
	private class ResolvableMetadataProvider implements MetadataProvider {

		private MetadataProvider delegate;

		public ResolvableMetadataProvider(MetadataProvider delegate) {
			this.delegate = delegate;
		}

		@Override
		public Map<Object, Object> getDefaults() {
			return this.delegate.getDefaults();
		}

		@Override
		public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
			return new ResolvableAnnotationReader(
					this.delegate.getAnnotationReader(annotatedElement));
		}

	}

	/**
	 * {@link AnnotationReader} decorator to also resolve environment place holders.
	 */
	private class ResolvableAnnotationReader implements AnnotationReader {

		private final AnnotationReader delegate;

		public ResolvableAnnotationReader(AnnotationReader delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
			return this.delegate.isAnnotationPresent(annotationType);
		}

		@Override
		public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
			return makeResolvable(this.delegate.getAnnotation(annotationType));
		}

		@Override
		public Annotation[] getAnnotations() {
			Annotation[] annotations = this.delegate.getAnnotations();
			for (int i = 0; i < annotations.length; i++) {
				annotations[i] = makeResolvable(annotations[i]);
			}
			return annotations;
		}

		private <T extends Annotation> T makeResolvable(T annotation) {
			if (annotation == null) {
				return null;
			}
			ResolvableAnnotation resolvable = PropertyPlaceholderHibernateConfigurationPostProcessor.this.resolvableAnnotations
					.get(annotation.annotationType());
			if (resolvable != null) {
				return resolvable.getResolvableVersion(annotation);
			}
			return annotation;
		}

	}

}
