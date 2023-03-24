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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.Map.Entry;
import java.util.Set;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for R2DBC.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.3.0
 */
@AutoConfiguration(before = { DataSourceAutoConfiguration.class, SqlInitializationAutoConfiguration.class })
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnResource(resources = "classpath:META-INF/services/io.r2dbc.spi.ConnectionFactoryProvider")
@EnableConfigurationProperties(R2dbcProperties.class)
@Import({ ConnectionFactoryConfigurations.PoolConfiguration.class,
		ConnectionFactoryConfigurations.GenericConfiguration.class, ConnectionFactoryDependentConfiguration.class })
public class R2dbcAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(R2dbcConnectionDetails.class)
	@ConditionalOnProperty("spring.r2dbc.url")
	PropertiesR2dbcConnectionDetails propertiesR2dbcConnectionDetails(R2dbcProperties properties) {
		return new PropertiesR2dbcConnectionDetails(properties);
	}

	/**
	 * Adapts {@link R2dbcProperties} to {@link R2dbcConnectionDetails}.
	 */
	static class PropertiesR2dbcConnectionDetails implements R2dbcConnectionDetails {

		private final R2dbcProperties properties;

		PropertiesR2dbcConnectionDetails(R2dbcProperties properties) {
			this.properties = properties;
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			StringBuilder url = new StringBuilder(this.properties.getUrl());
			Set<Entry<String, String>> options = this.properties.getProperties().entrySet();
			if (!options.isEmpty()) {
				url.append("?");
				url.append(String.join("&",
						options.stream().map((option) -> option.getKey() + "=" + option.getValue()).toList()));
			}
			return ConnectionFactoryOptions.parse(url);
		}

	}

}
