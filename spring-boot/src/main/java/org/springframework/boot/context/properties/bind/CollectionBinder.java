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

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.CollectionFactory;
import org.springframework.core.ResolvableType;

/**
 * {@link AggregateBinder} for collections.
 *
 * @author Phillip Webb
 */
class CollectionBinder extends IndexedElementsBinder<Collection<Object>> {

	CollectionBinder(BindContext context) {
		super(context);
	}

	@Override
	protected Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder itemBinder, Class<?> type) {
		AggregateSupplier<Collection<Object>> collection = new AggregateSupplier<>(
				() -> CollectionFactory.createCollection(type, 0));
		ResolvableType elementType = target.getType().asCollection().getGeneric();
		return bindToCollection(name, target, itemBinder, collection, target.getType(),
				elementType);
	}

	protected final <E> Collection<E> bindToCollection(ConfigurationPropertyName name,
			Bindable<?> target, AggregateElementBinder itemBinder,
			AggregateSupplier<? extends Collection<E>> collection,
			ResolvableType collectionType, ResolvableType elementType) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			bindToCollection(source, name, target, itemBinder, collection, collectionType,
					elementType);
			if (collection.wasSupplied() && collection.get() != null) {
				return collection.get();
			}
		}
		return (!collection.wasSupplied() || collection.get() == null ? null
				: collection.get());
	}

	private <E> void bindToCollection(ConfigurationPropertySource source,
			ConfigurationPropertyName root, Bindable<?> target,
			AggregateElementBinder elementBinder,
			AggregateSupplier<? extends Collection<E>> collection,
			ResolvableType collectionType, ResolvableType elementType) {
		ConfigurationProperty property = source.getConfigurationProperty(root);
		if (property != null) {
			collection.get().addAll(convert(property, collectionType));
		}
		else {
			bindIndexed(source, root, elementBinder, collection, elementType);
		}
	}

	@SuppressWarnings("unchecked")
	private <E> Collection<E> convert(ConfigurationProperty property,
			ResolvableType type) {
		Object value = property.getValue();
		value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
		BinderConversionService conversionService = getContext().getConversionService();
		return (Collection<E>) conversionService.convert(value, type);
	}

	@Override
	protected Collection<Object> merge(Collection<Object> existing,
			Collection<Object> additional) {
		existing.clear();
		existing.addAll(additional);
		return existing;
	}

}
