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

import java.security.KeyStore;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.ssl.SslStoreProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslAutoConfiguration}.
 *
 * @author Scott Frederick
 */
class SslAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class));

	@Test
	void sslBundlesCreatedWithNoConfiguration() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(SslBundleRegistry.class));
	}

	@Test
	void sslBundlesCreatedWithCertificates() {
		this.contextRunner.withPropertyValues("spring.ssl.certificate.first.certificate=cert1.pem",
				"spring.ssl.certificate.first.certificate-private-key=key1.pem",
				"spring.ssl.certificate.first.key-alias=alias1", "spring.ssl.certificate.first.key-password=secret1",
				"spring.ssl.certificate.first.key-store-type=JKS",
				"spring.ssl.certificate.first.trust-store-type=PKCS12",
				"spring.ssl.certificate.second.certificate=cert2.pem",
				"spring.ssl.certificate.second.certificate-private-key=key2.pem",
				"spring.ssl.certificate.second.trust-certificate=ca.pem",
				"spring.ssl.certificate.second.trust-certificate-private-key=ca-key.pem",
				"spring.ssl.certificate.second.key-alias=alias2", "spring.ssl.certificate.second.key-password=secret2",
				"spring.ssl.certificate.second.key-store-type=PKCS12",
				"spring.ssl.certificate.second.trust-store-type=JKS")
			.run((context) -> {
				assertThat(context).hasSingleBean(SslBundles.class);
				SslBundles bundles = context.getBean(SslBundles.class);
				SslBundle first = bundles.getBundle("first");
				assertThat(first).isNotNull();
				assertThat(first.getKeyStores()).isNotNull();
				assertThat(first.getManagers()).isNotNull();
				assertThat(first.getDetails().getKeyAlias()).isEqualTo("alias1");
				assertThat(first.getDetails().getKeyPassword()).isEqualTo("secret1");
				assertThat(first.getDetails().getKeyStoreType()).isEqualTo("JKS");
				assertThat(first.getDetails().getTrustStoreType()).isEqualTo("PKCS12");
				SslBundle second = bundles.getBundle("second");
				assertThat(second).isNotNull();
				assertThat(second.getKeyStores()).isNotNull();
				assertThat(second.getManagers()).isNotNull();
				assertThat(second.getDetails().getKeyAlias()).isEqualTo("alias2");
				assertThat(second.getDetails().getKeyPassword()).isEqualTo("secret2");
				assertThat(second.getDetails().getKeyStoreType()).isEqualTo("PKCS12");
				assertThat(second.getDetails().getTrustStoreType()).isEqualTo("JKS");
			});
	}

	@Test
	void sslBundlesCreatedWithJavaKeyStores() {
		this.contextRunner.withPropertyValues("spring.ssl.keystore.first.key-store=first.p12",
				"spring.ssl.keystore.first.trust-store=first-ca.p12", "spring.ssl.keystore.first.key-alias=alias1",
				"spring.ssl.keystore.first.key-password=secret1", "spring.ssl.keystore.first.key-store-type=JKS",
				"spring.ssl.keystore.first.trust-store-type=PKCS12", "spring.ssl.keystore.second.key-store=second.p12",
				"spring.ssl.keystore.second.trust-store=second-ca.p12", "spring.ssl.keystore.second.key-alias=alias2",
				"spring.ssl.keystore.second.key-password=secret2", "spring.ssl.keystore.second.key-store-type=PKCS12",
				"spring.ssl.keystore.second.trust-store-type=JKS")
			.run((context) -> {
				assertThat(context).hasSingleBean(SslBundles.class);
				SslBundles bundles = context.getBean(SslBundles.class);
				SslBundle first = bundles.getBundle("first");
				assertThat(first).isNotNull();
				assertThat(first.getKeyStores()).isNotNull();
				assertThat(first.getManagers()).isNotNull();
				assertThat(first.getDetails().getKeyAlias()).isEqualTo("alias1");
				assertThat(first.getDetails().getKeyPassword()).isEqualTo("secret1");
				assertThat(first.getDetails().getKeyStoreType()).isEqualTo("JKS");
				assertThat(first.getDetails().getTrustStoreType()).isEqualTo("PKCS12");
				SslBundle second = bundles.getBundle("second");
				assertThat(second).isNotNull();
				assertThat(second.getKeyStores()).isNotNull();
				assertThat(second.getManagers()).isNotNull();
				assertThat(second.getDetails().getKeyAlias()).isEqualTo("alias2");
				assertThat(second.getDetails().getKeyPassword()).isEqualTo("secret2");
				assertThat(second.getDetails().getKeyStoreType()).isEqualTo("PKCS12");
				assertThat(second.getDetails().getTrustStoreType()).isEqualTo("JKS");
			});
	}

	@Test
	void sslBundlesCreatedWithCustomSslBundle() {
		this.contextRunner.withUserConfiguration(CustomSslBundleConfiguration.class)
			.withPropertyValues("custom.ssl.key-alias=alias1", "custom.ssl.key-password=secret1",
					"custom.ssl.key-store-type=JKS", "custom.ssl.trust-store-type=PKCS12")
			.run((context) -> {
				assertThat(context).hasSingleBean(SslBundles.class);
				SslBundles bundles = context.getBean(SslBundles.class);
				SslBundle first = bundles.getBundle("custom");
				assertThat(first).isNotNull();
				assertThat(first.getKeyStores()).isNotNull();
				assertThat(first.getKeyStores().getKeyStore()).isNotNull();
				assertThat(first.getKeyStores().getTrustStore()).isNotNull();
				assertThat(first.getManagers()).isNotNull();
				assertThat(first.getDetails().getKeyAlias()).isEqualTo("alias1");
				assertThat(first.getDetails().getKeyPassword()).isEqualTo("secret1");
				assertThat(first.getDetails().getKeyStoreType()).isEqualTo("JKS");
				assertThat(first.getDetails().getTrustStoreType()).isEqualTo("PKCS12");
			});
	}

	@Configuration
	@EnableConfigurationProperties(CustomSslProperties.class)
	public static class CustomSslBundleConfiguration {

		@Bean
		public SslBundleRegistrar customSslBundlesRegistrar(CustomSslProperties properties) {
			return new CustomSslBundlesRegistrar(properties);
		}

	}

	@ConfigurationProperties("custom.ssl")
	static class CustomSslProperties extends SslDetails {

	}

	static class CustomSslBundlesRegistrar implements SslBundleRegistrar {

		private final CustomSslProperties properties;

		CustomSslBundlesRegistrar(CustomSslProperties properties) {
			this.properties = properties;
		}

		@Override
		public void registerBundles(SslBundleRegistry registry) {
			SslBundle bundle = new SslBundle(this.properties, new CustomSslStoreProvider());
			registry.registerBundle("custom", bundle);
		}

	}

	static class CustomSslStoreProvider implements SslStoreProvider {

		@Override
		public KeyStore getKeyStore() throws Exception {
			return KeyStore.getInstance("PKCS12");
		}

		@Override
		public KeyStore getTrustStore() throws Exception {
			return KeyStore.getInstance("PKCS12");
		}

	}

}
