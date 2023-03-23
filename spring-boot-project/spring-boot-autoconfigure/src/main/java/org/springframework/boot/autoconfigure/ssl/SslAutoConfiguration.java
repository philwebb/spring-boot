/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SSL bundles.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SslProperties.class)
public class SslAutoConfiguration {

	@Bean
	public SslPropertiesSslBundleRegistrar sslPropertiesSslBundleRegistrar(SslProperties sslProperties) {
		return new SslPropertiesSslBundleRegistrar(sslProperties);
	}

	@Bean
	@ConditionalOnMissingBean(SslBundles.class)
	public SslBundleRegistry sslBundleRegistry(List<SslBundleRegistrar> sslBundleRegistrars) {
		SslBundleRegistry bundleRegistry = new SslBundleRegistry();
		sslBundleRegistrars.forEach((registrar) -> registrar.registerBundles(bundleRegistry));
		return bundleRegistry;
	}

}
