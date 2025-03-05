/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.client;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.springframework.boot.selector.AbstractSelectableSet;
import org.springframework.boot.selector.DuplicateSelectableNameException;
import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.SelectableSet.ElementProvider.Scope;
import org.springframework.boot.web.client.RestClientBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

class AutoConfiguredRestClientBuilders extends AbstractSelectableSet<RestClientBuilders, RestClient.Builder>
		implements RestClientBuilders {

	AutoConfiguredRestClientBuilders(RestClientProperties properties) {
		this(properties, null, null, null);
	}

	/**
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param selectableProvider
	 * @param elementProvider
	 * @param elementScope
	 * @param elementPostProcessor
	 * @throws DuplicateSelectableNameException
	 */
	public <K, V> AutoConfiguredRestClientBuilders(Map<K, V> map,
			BiFunction<? super K, ? super V, Selectable> selectableProvider,
			BiFunction<? super K, ? super V, Builder> elementProvider, Scope elementScope,
			UnaryOperator<Builder> elementPostProcessor) throws DuplicateSelectableNameException {
		super(map, selectableProvider, elementProvider, elementScope, elementPostProcessor);
		// TODO Auto-generated constructor stub
	}

	private static Selectable asSelectable(String name, RestClientProperties properties) {
		return null;
	}

	private static RestClient.Builder asRestClientBuilder(String name, RestClientProperties x) {
		return null;
	}

	@Override
	protected RestClientBuilders withPredicate(Map<String, Entry<Builder>> entries,
			Predicate<Entry<Builder>> predicate) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
