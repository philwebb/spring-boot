/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.bind;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Base class for {@link AggregateBinder AggregateBinders} that read a sequential run of
 * indexed items.
 *
 * @param <T> the type being bound
 * @author Phillip Webb
 */
abstract class IndexedElementsBinder<T> extends AggregateBinder<T> {

	IndexedElementsBinder(BindContext context) {
		super(context);
	}

	@SuppressWarnings("unchecked")
	protected final <E> void bindIndexed(ConfigurationPropertySource source,
			ConfigurationPropertyName root, AggregateElementBinder elementBinder,
			AggregateSupplier<? extends Collection<E>> collection,
			ResolvableType elementType) {
		MultiValueMap<String, ConfigurationProperty> knownIndexedChildren = getKnownIndexedChildren(
				source, root);
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			ConfigurationPropertyName name = root.append("[" + i + "]");
			E value = (E) elementBinder.bind(name, Bindable.of(elementType), source);
			if (value == null) {
				break;
			}
			knownIndexedChildren.remove(name.getElement().getValue(Form.UNIFORM));
			collection.get().add(value);
		}
		assertNoUnboundChildren(knownIndexedChildren);
	}

	protected final MultiValueMap<String, ConfigurationProperty> getKnownIndexedChildren(
			ConfigurationPropertySource source, ConfigurationPropertyName root) {
		MultiValueMap<String, ConfigurationProperty> children = new LinkedMultiValueMap<>();
		for (ConfigurationPropertyName name : source.filter(root::isAncestorOf)) {
			name = rollUp(name, root);
			if (name.getElement().isIndexed()) {
				String key = name.getElement().getValue(Form.UNIFORM);
				ConfigurationProperty value = source.getConfigurationProperty(name);
				children.add(key, value);
			}
		}
		return children;
	}

	protected final void assertNoUnboundChildren(
			MultiValueMap<String, ConfigurationProperty> children) {
		if (!children.isEmpty()) {
			throw new UnboundConfigurationPropertiesException(
					children.values().stream().flatMap(List::stream)
							.collect(Collectors.toCollection(TreeSet::new)));
		}
	}

	@SuppressWarnings("unchecked")
	protected <C> C convert(ConfigurationProperty property, ResolvableType type) {
		Object value = property.getValue();
		value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
		BinderConversionService conversionService = getContext().getConversionService();
		return (C) conversionService.convert(value, type);
	}

}
