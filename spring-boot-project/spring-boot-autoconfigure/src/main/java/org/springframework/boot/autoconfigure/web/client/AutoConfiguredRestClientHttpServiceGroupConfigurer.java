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

import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.web.service.invoker.HttpServiceProxyFactoryCustomizer;
import org.springframework.boot.selector.NoSuchSelectableNameException;
import org.springframework.boot.selector.Selectable;
import org.springframework.boot.selector.Selector;
import org.springframework.boot.web.client.RestClientBuilders;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.service.registry.HttpServiceGroup;

/**
 * AutoConfigured {@link RestClientHttpServiceGroupConfigurer} to apply
 * {@link HttpServiceProxyFactoryCustomizer} beans.
 *
 * @author Phillip Webb
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class AutoConfiguredRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	private final Environment environment;

	private final RestClientBuilders restClientBuilders;

	private final List<HttpServiceProxyFactoryCustomizer> proxyFactoryCustomizers;

	AutoConfiguredRestClientHttpServiceGroupConfigurer(Environment environment, RestClientBuilders restClientBuilders,
			List<HttpServiceProxyFactoryCustomizer> proxyFactoryCustomizers) {
		this.environment = environment;
		this.restClientBuilders = restClientBuilders;
		this.proxyFactoryCustomizers = proxyFactoryCustomizers;
	}

	@Override
	public void configureGroups(Groups<Builder> groups) {
		groups.configureProxyFactory(this::configureProxyFactory);
	}

	private void configureProxyFactory(HttpServiceGroup group, HttpServiceProxyFactory.Builder builder) {
		builder.embeddedValueResolver((value) -> resolveEmbeddedValue(value, group));
		Selectable selectable = asSelectable(group);
		streamProxyFactoryCustomizers(selectable).forEach((customizer) -> customizer.customize(builder));
	}

	private String resolveEmbeddedValue(String value, HttpServiceGroup group) {
		if (value.equals("@" + group.name())) {
			return "";
		}
		return this.environment.resolvePlaceholders(value);
	}

	private Stream<HttpServiceProxyFactoryCustomizer> streamProxyFactoryCustomizers(Selectable selectable) {
		return (selectable != null) ? Selector.streamSelected(this.proxyFactoryCustomizers, selectable)
				: this.proxyFactoryCustomizers.stream().filter(Selector.selectingBlank());
	}

	private Selectable asSelectable(HttpServiceGroup group) {
		try {
			return this.restClientBuilders.getEntry(group.name()).selectable();
		}
		catch (NoSuchSelectableNameException ex) {
			return null;
		}
	}

}
