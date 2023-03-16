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

package org.springframework.boot.autoconfigure.influx;

import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.impl.InfluxDBImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnClass(InfluxDB.class)
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(InfluxDBCondition.class)
	public InfluxDB influxDb(InfluxDbProperties properties, ObjectProvider<InfluxDbOkHttpClientBuilderProvider> builder,
			ObjectProvider<InfluxDbCustomizer> customizers,
			ObjectProvider<InfluxDbServiceConnection> serviceConnectionProvider) {
		InfluxDbServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
		String url = (serviceConnection != null) ? serviceConnection.getUrl().toString() : properties.getUrl();
		String user = (serviceConnection != null) ? serviceConnection.getUsername() : properties.getUser();
		String password = (serviceConnection != null) ? serviceConnection.getPassword() : properties.getPassword();
		InfluxDB influxDb = new InfluxDBImpl(url, user, password, determineBuilder(builder.getIfAvailable()));
		customizers.orderedStream().forEach((customizer) -> customizer.customize(influxDb));
		return influxDb;
	}

	private static OkHttpClient.Builder determineBuilder(InfluxDbOkHttpClientBuilderProvider builder) {
		if (builder != null) {
			return builder.get();
		}
		return new OkHttpClient.Builder();
	}

	static final class InfluxDBCondition extends AnyNestedCondition {

		InfluxDBCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "spring.influx", name = "url")
		private static final class InfluxUrlCondition {

		}

		@ConditionalOnBean(InfluxDbServiceConnection.class)
		private static final class InfluxDbServiceConnectionCondition {

		}

	}

}
