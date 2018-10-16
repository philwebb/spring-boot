/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * Provides access to relevant details of an auto-configuration class.
 *
 * @author Phillip Webb
 */
class AutoConfigurationClass {

	private static final String[] NAME_AND_VALUE = { "name", "value" };

	private final String className;

	private final MetadataReaderFactory metadataReaderFactory;

	private final AutoConfigurationMetadata autoConfigurationMetadata;

	private volatile AnnotationMetadata annotationMetadata;

	private volatile Set<String> before;

	private volatile Set<String> after;

	private volatile Set<String> deprecatedReplacements;

	AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata) {
		this.className = className;
		this.metadataReaderFactory = metadataReaderFactory;
		this.autoConfigurationMetadata = autoConfigurationMetadata;
	}

	public boolean isAvailable() {
		try {
			if (!wasProcessed()) {
				getAnnotationMetadata();
			}
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

	public Set<String> getBefore() {
		if (this.before == null) {
			this.before = (wasProcessed()
					? this.autoConfigurationMetadata.getSet(this.className,
							"AutoConfigureBefore", Collections.emptySet())
					: getAnnotationValue(AutoConfigureBefore.class));
		}
		return this.before;
	}

	public Set<String> getAfter() {
		if (this.after == null) {
			this.after = (wasProcessed()
					? this.autoConfigurationMetadata.getSet(this.className,
							"AutoConfigureAfter", Collections.emptySet())
					: getAnnotationValue(AutoConfigureAfter.class));
		}
		return this.after;
	}

	public int getOrder() {
		if (wasProcessed()) {
			return this.autoConfigurationMetadata.getInteger(this.className,
					"AutoConfigureOrder", AutoConfigureOrder.DEFAULT_ORDER);
		}
		Map<String, Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
		return (attributes != null) ? (Integer) attributes.get("value")
				: AutoConfigureOrder.DEFAULT_ORDER;
	}

	public Set<String> getDeprecatedReplacements() {
		if (this.deprecatedReplacements == null) {
			this.deprecatedReplacements = (wasProcessed()
					? this.autoConfigurationMetadata.getSet(this.className,
							"DeprecatedAutoConfiguration", Collections.emptySet())
					: getAnnotationValue(DeprecatedAutoConfiguration.class,
							"replacement"));
		}
		return this.deprecatedReplacements;
	}

	private boolean wasProcessed() {
		return (this.autoConfigurationMetadata != null
				&& this.autoConfigurationMetadata.wasProcessed(this.className));
	}

	private Set<String> getAnnotationValue(Class<?> annotation) {
		return getAnnotationValue(annotation, NAME_AND_VALUE);
	}

	private Set<String> getAnnotationValue(Class<?> annotation, String... names) {
		Map<String, Object> attributes = getAnnotationMetadata()
				.getAnnotationAttributes(annotation.getName(), true);
		if (attributes == null) {
			return Collections.emptySet();
		}
		Set<String> value = new LinkedHashSet<>();
		for (String name : names) {
			Collections.addAll(value, (String[]) attributes.get(name));
		}
		return value;
	}

	private AnnotationMetadata getAnnotationMetadata() {
		if (this.annotationMetadata == null) {
			try {
				MetadataReader metadataReader = this.metadataReaderFactory
						.getMetadataReader(this.className);
				this.annotationMetadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						"Unable to read meta-data for class " + this.className, ex);
			}
		}
		return this.annotationMetadata;
	}

}
