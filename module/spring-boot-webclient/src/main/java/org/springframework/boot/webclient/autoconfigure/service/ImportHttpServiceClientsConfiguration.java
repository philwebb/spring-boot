/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webclient.autoconfigure.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.http.client.autoconfigure.service.ConditionalOnMissingHttpServiceProxyBean;
import org.springframework.boot.http.client.service.HttpServiceClientScanRegistrar;
import org.springframework.boot.webclient.autoconfigure.service.ImportHttpServiceClientsConfiguration.Registrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;

/**
 * {@link Configuration @Configuration} to import HTTP Service clients when no
 * user-defined HTTP service client beans are found.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingHttpServiceProxyBean
@Import(Registrar.class)
class ImportHttpServiceClientsConfiguration {

	/**
	 * {@link HttpServiceClientScanRegistrar} backed by {@link AutoConfigurationPackages}.
	 */
	static class Registrar extends HttpServiceClientScanRegistrar {

		Registrar(BeanFactory beanFactory) {
			super(ClientType.WEB_CLIENT, () -> getAutoConfigurationPackages(beanFactory));
		}

		private static List<String> getAutoConfigurationPackages(BeanFactory beanFactory) {
			return (AutoConfigurationPackages.has(beanFactory)) ? AutoConfigurationPackages.get(beanFactory)
					: Collections.emptyList();
		}

	}

}
