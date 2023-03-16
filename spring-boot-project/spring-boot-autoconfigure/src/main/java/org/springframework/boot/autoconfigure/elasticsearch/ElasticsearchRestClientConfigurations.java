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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.stream.Stream;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchServiceConnection.Node;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch rest client configurations.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Moritz Halbritter
 */
class ElasticsearchRestClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClientBuilder.class)
	static class RestClientBuilderConfiguration {

		private final ElasticsearchProperties properties;

		RestClientBuilderConfiguration(ElasticsearchProperties properties) {
			this.properties = properties;
		}

		@Bean
		RestClientBuilderCustomizer defaultRestClientBuilderCustomizer(
				ObjectProvider<ElasticsearchServiceConnection> serviceConnectionProvider) {
			return new DefaultRestClientBuilderCustomizer(this.properties, serviceConnectionProvider.getIfAvailable());
		}

		@Bean
		RestClientBuilder elasticsearchRestClientBuilder(ObjectProvider<RestClientBuilderCustomizer> builderCustomizers,
				ObjectProvider<ElasticsearchServiceConnection> serviceConnectionProvider) {
			ElasticsearchServiceConnection serviceConnection = serviceConnectionProvider.getIfAvailable();
			HttpHost[] hosts = (serviceConnection != null) ? getHosts(serviceConnection) : getHosts(this.properties);
			RestClientBuilder builder = RestClient.builder(hosts);
			builder.setHttpClientConfigCallback((httpClientBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
				return httpClientBuilder;
			});
			builder.setRequestConfigCallback((requestConfigBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
				return requestConfigBuilder;
			});
			String pathPrefix = (serviceConnection != null) ? serviceConnection.getPathPrefix()
					: this.properties.getPathPrefix();
			if (pathPrefix != null) {
				builder.setPathPrefix(pathPrefix);
			}
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

		private HttpHost[] getHosts(ElasticsearchProperties properties) {
			return properties.getUris().stream().map(this::createHttpHost).toArray(HttpHost[]::new);
		}

		private HttpHost[] getHosts(ElasticsearchServiceConnection serviceConnection) {
			return serviceConnection.getNodes()
				.stream()
				.map((node) -> new HttpHost(node.hostname(), node.port(), node.protocol().getScheme()))
				.toArray(HttpHost[]::new);
		}

		private HttpHost createHttpHost(String uri) {
			try {
				return createHttpHost(URI.create(uri));
			}
			catch (IllegalArgumentException ex) {
				return HttpHost.create(uri);
			}
		}

		private HttpHost createHttpHost(URI uri) {
			if (!StringUtils.hasLength(uri.getUserInfo())) {
				return HttpHost.create(uri.toString());
			}
			try {
				return HttpHost.create(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
						uri.getQuery(), uri.getFragment())
					.toString());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClient.class)
	static class RestClientConfiguration {

		@Bean
		RestClient elasticsearchRestClient(RestClientBuilder restClientBuilder) {
			return restClientBuilder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Sniffer.class)
	@ConditionalOnSingleCandidate(RestClient.class)
	static class RestClientSnifferConfiguration {

		@Bean
		@ConditionalOnMissingBean
		Sniffer elasticsearchSniffer(RestClient client, ElasticsearchProperties properties) {
			SnifferBuilder builder = Sniffer.builder(client);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			Duration interval = properties.getRestclient().getSniffer().getInterval();
			map.from(interval).asInt(Duration::toMillis).to(builder::setSniffIntervalMillis);
			Duration delayAfterFailure = properties.getRestclient().getSniffer().getDelayAfterFailure();
			map.from(delayAfterFailure).asInt(Duration::toMillis).to(builder::setSniffAfterFailureDelayMillis);
			return builder.build();
		}

	}

	static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchProperties properties;

		private final ElasticsearchServiceConnection serviceConnection;

		DefaultRestClientBuilderCustomizer(ElasticsearchProperties properties,
				ElasticsearchServiceConnection serviceConnection) {
			this.properties = properties;
			this.serviceConnection = serviceConnection;
		}

		@Override
		public void customize(RestClientBuilder builder) {
		}

		@Override
		public void customize(HttpAsyncClientBuilder builder) {
			builder.setDefaultCredentialsProvider(
					new PropertiesCredentialsProvider(this.properties, this.serviceConnection));
			map.from(this.properties::isSocketKeepAlive)
				.to((keepAlive) -> builder
					.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(keepAlive).build()));
		}

		@Override
		public void customize(RequestConfig.Builder builder) {
			map.from(this.properties::getConnectionTimeout)
				.whenNonNull()
				.asInt(Duration::toMillis)
				.to(builder::setConnectTimeout);
			map.from(this.properties::getSocketTimeout)
				.whenNonNull()
				.asInt(Duration::toMillis)
				.to(builder::setSocketTimeout);
		}

	}

	private static class PropertiesCredentialsProvider extends BasicCredentialsProvider {

		PropertiesCredentialsProvider(ElasticsearchProperties properties,
				ElasticsearchServiceConnection serviceConnection) {
			String username = (serviceConnection != null) ? serviceConnection.getUsername() : properties.getUsername();
			String password = (serviceConnection != null) ? serviceConnection.getPassword() : properties.getPassword();
			if (StringUtils.hasText(username)) {
				Credentials credentials = new UsernamePasswordCredentials(username, password);
				setCredentials(AuthScope.ANY, credentials);
			}
			Stream<URI> uris = (serviceConnection != null) ? getUris(serviceConnection) : getUris(properties);
			uris.filter(this::hasUserInfo).forEach(this::addUserInfoCredentials);
		}

		private Stream<URI> getUris(ElasticsearchProperties properties) {
			return properties.getUris().stream().map(this::toUri);
		}

		private Stream<URI> getUris(ElasticsearchServiceConnection serviceConnection) {
			return serviceConnection.getNodes().stream().map(Node::toUri);
		}

		private URI toUri(String uri) {
			try {
				return URI.create(uri);
			}
			catch (IllegalArgumentException ex) {
				return null;
			}
		}

		private boolean hasUserInfo(URI uri) {
			return uri != null && StringUtils.hasLength(uri.getUserInfo());
		}

		private void addUserInfoCredentials(URI uri) {
			AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
			Credentials credentials = createUserInfoCredentials(uri.getUserInfo());
			setCredentials(authScope, credentials);
		}

		private Credentials createUserInfoCredentials(String userInfo) {
			int delimiter = userInfo.indexOf(":");
			if (delimiter == -1) {
				return new UsernamePasswordCredentials(userInfo, null);
			}
			String username = userInfo.substring(0, delimiter);
			String password = userInfo.substring(delimiter + 1);
			return new UsernamePasswordCredentials(username, password);
		}

	}

}
