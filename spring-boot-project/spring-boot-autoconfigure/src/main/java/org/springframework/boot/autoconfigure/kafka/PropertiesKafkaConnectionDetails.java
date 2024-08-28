/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.util.List;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link KafkaProperties} to {@link KafkaConnectionDetails}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PropertiesKafkaConnectionDetails implements KafkaConnectionDetails {

	private final KafkaProperties properties;

	private final SslBundles sslBundles;

	PropertiesKafkaConnectionDetails(KafkaProperties properties, SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public List<String> getBootstrapServers() {
		return this.properties.getBootstrapServers();
	}

	@Override
	public List<String> getConsumerBootstrapServers() {
		return getServers(this.properties.getConsumer().getBootstrapServers());
	}

	@Override
	public SslBundle getConsumerSslBundle() {
		SslBundle sslBundle = getBundle(this.properties.getConsumer().getSsl());
		return (sslBundle != null) ? sslBundle : getBundle(this.properties.getSsl());
	}

	@Override
	public List<String> getProducerBootstrapServers() {
		return getServers(this.properties.getProducer().getBootstrapServers());
	}

	@Override
	public SslBundle getProducerSslBundle() {
		SslBundle sslBundle = getBundle(this.properties.getProducer().getSsl());
		return (sslBundle != null) ? sslBundle : getBundle(this.properties.getSsl());
	}

	@Override
	public List<String> getStreamsBootstrapServers() {
		return getServers(this.properties.getStreams().getBootstrapServers());
	}

	@Override
	public SslBundle getStreamsSslBundle() {
		SslBundle sslBundle = getBundle(this.properties.getStreams().getSsl());
		return (sslBundle != null) ? sslBundle : getBundle(this.properties.getSsl());
	}

	@Override
	public SslBundle getAdminSslBundle() {
		SslBundle sslBundle = getBundle(this.properties.getAdmin().getSsl());
		return (sslBundle != null) ? sslBundle : getBundle(this.properties.getSsl());
	}

	private List<String> getServers(List<String> servers) {
		return (servers != null) ? servers : getBootstrapServers();
	}

	private SslBundle getBundle(Ssl ssl) {
		if (StringUtils.hasLength(ssl.getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(ssl.getBundle());
		}
		return null;
	}

}
