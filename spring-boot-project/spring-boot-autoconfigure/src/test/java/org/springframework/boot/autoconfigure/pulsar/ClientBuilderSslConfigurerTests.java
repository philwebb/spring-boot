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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.pulsar.ClientBuilderSslConfigurer.SslType;
import org.springframework.boot.autoconfigure.ssl.JksSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.SslProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link ClientBuilderSslConfigurer}. The approach is to mock the SSL
 * bundles and SSL properties and verify the proper methods are invoked on the underlying
 * Pulsar {@code ClientBuilder}.
 *
 * @author Chris Bono
 */
class ClientBuilderSslConfigurerTests {

	private SslBundles sslBundles;

	private SslProperties systemSslProperties;

	private ClientBuilderSslConfigurer clientBuilderSslConfigurer;

	@BeforeEach
	void prepareForTest() {
		JksSslBundleProperties jksProperties = new JksSslBundleProperties();
		jksProperties.getKeystore().setLocation("/foo-key.p12");
		jksProperties.getKeystore().setPassword("pwd1");
		jksProperties.getKeystore().setType("PKCS12");
		jksProperties.getKeystore().setProvider("Sun JVM");
		jksProperties.getTruststore().setLocation("/foo-trust.p12");

		jksProperties.getTruststore().setPassword("pwd2");
		jksProperties.getTruststore().setType("JKS");
		jksProperties.getTruststore().setProvider("Sun JVM");

		PemSslBundleProperties pemProperties = new PemSslBundleProperties();
		pemProperties.getKeystore().setCertificate("/foo.crt");
		pemProperties.getKeystore().setPrivateKey("/foo.key");
		pemProperties.getKeystore().setPrivateKeyPassword("pwd");
		pemProperties.getTruststore().setCertificate("/foo-trust.crt");

		this.systemSslProperties = new SslProperties();
		this.systemSslProperties.getBundle().getJks().put("foo", jksProperties);
		this.systemSslProperties.getBundle().getPem().put("bar", pemProperties);
		this.sslBundles = mock(SslBundles.class);
		this.clientBuilderSslConfigurer = new ClientBuilderSslConfigurer(this.sslBundles, this.systemSslProperties);
	}

	@Test
	void sslDisabled() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(false);
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties);
		then(clientBuilderSslSettings).shouldHaveNoInteractions();
	}

	@SuppressWarnings("deprecation")
	@Test
	void sslEnabledWithNoBundleSpecified() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setVerifyHostname(true);
		appSslProperties.setAllowInsecureConnection(true);
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties);
		then(clientBuilderSslSettings).should().enableTls(true);
		then(clientBuilderSslSettings).should().enableTlsHostnameVerification(true);
		then(clientBuilderSslSettings).should().allowTlsInsecureConnection(true);
		then(clientBuilderSslSettings).shouldHaveNoMoreInteractions();
	}

	@Test
	void sslEnabledWithBundleSpecifiedButNoBundlesConfigured() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("foo");
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer = new ClientBuilderSslConfigurer(null, this.systemSslProperties);
		assertThatIllegalStateException()
			.isThrownBy(() -> this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties))
			.withMessage("SSL enabled but no SSL bundles configured");
	}

	@Test
	void sslEnabledWithBundleSpecifiedButNoSslPropertiesConfigured() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("foo");
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer = new ClientBuilderSslConfigurer(this.sslBundles, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties))
			.withMessage("SSL enabled but no SSL properties configured");
	}

	@Test
	void jksBundleType() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("foo");
		setupJskBundleForBundleName("foo");
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties);
		then(clientBuilderSslSettings).should().useKeyStoreTls(true);
		then(clientBuilderSslSettings).should().tlsKeyStoreType("PKCS12");
		then(clientBuilderSslSettings).should().tlsKeyStorePath(endsWith("/foo-key.p12"));
		then(clientBuilderSslSettings).should().tlsKeyStorePassword("pwd1");
		then(clientBuilderSslSettings).should().tlsTrustStoreType("JKS");
		then(clientBuilderSslSettings).should().tlsTrustStorePath(endsWith("/foo-trust.p12"));
		then(clientBuilderSslSettings).should().tlsTrustStorePassword("pwd2");
		then(clientBuilderSslSettings).should(times(2)).sslProvider("Sun JVM");
	}

	@Test
	void pemBundleType() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("bar");
		setupPemBundleForBundleName("bar");
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties);
		then(clientBuilderSslSettings).should(never()).useKeyStoreTls(true);
		then(clientBuilderSslSettings).should().tlsCertificateFilePath("/foo.crt");
		then(clientBuilderSslSettings).should().tlsKeyFilePath("/foo.key");
		then(clientBuilderSslSettings).should().tlsTrustCertsFilePath("/foo-trust.crt");
	}

	@Test
	void unsupportedBundleType() {
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("bar");
		setupBundleForBundleName("bar", SslStoreBundle.NONE);
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties))
			.withMessage("Unsupported store bundle type %s".formatted(SslStoreBundle.NONE.getClass().getName()));
	}

	@Test
	void locationsResolvedProperly() {
		PemSslBundleProperties pemProperties = new PemSslBundleProperties();

		// classpath -> resolved file under resources dir
		pemProperties.getKeystore().setCertificate("classpath:pulsar/foo.crt");

		// relative path -> resolved to absolute path of working dir
		pemProperties.getKeystore().setPrivateKey("foo.key");

		// absolute path -> left as-is
		pemProperties.getTruststore().setCertificate("/foo-trust.crt");

		this.systemSslProperties.getBundle().getPem().put("zaa", pemProperties);
		SslConfigProperties appSslProperties = new SslConfigProperties();
		appSslProperties.setEnabled(true);
		appSslProperties.setBundle("zaa");
		setupPemBundleForBundleName("zaa");
		ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
		this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings, appSslProperties);

		then(clientBuilderSslSettings).should().tlsCertificateFilePath(endsWith("/resources/test/pulsar/foo.crt"));
		then(clientBuilderSslSettings).should()
			.tlsKeyFilePath(endsWith("/spring-boot-project/spring-boot-autoconfigure/foo.key"));
		then(clientBuilderSslSettings).should().tlsTrustCertsFilePath("/foo-trust.crt");

	}

	private SslBundle setupJskBundleForBundleName(String bundleName) {
		// This ensures "<bundleName>" -> JskSslStoreBundle (signals JKS to configurer)
		return setupBundleForBundleName(bundleName, new JksSslStoreBundle(null, null));
	}

	private SslBundle setupPemBundleForBundleName(String bundleName) {
		// This ensures "<bundleName>" -> PemSslStoreBundle (signals PEM to configurer)
		return setupBundleForBundleName(bundleName, new PemSslStoreBundle(null, null));
	}

	private SslBundle setupBundleForBundleName(String bundleName, SslStoreBundle bundle) {
		SslBundle sslBundle = mock(SslBundle.class, Mockito.RETURNS_DEEP_STUBS);
		given(sslBundle.getStores()).willReturn(bundle);
		given(this.sslBundles.getBundle(bundleName)).willReturn(sslBundle);
		return sslBundle;
	}

	@Nested
	class SslOptionsTests {

		@Test
		void singleCipherOption() {
			SslConfigProperties appSslProperties = new SslConfigProperties();
			appSslProperties.setEnabled(true);
			appSslProperties.setBundle("foo");
			SslBundle sslBundle = setupJskBundleForBundleName("foo");
			Set<String> ciphers = Set.of("cipher1");
			given(sslBundle.getOptions()).willReturn(SslOptions.of(ciphers, null));
			ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
			ClientBuilderSslConfigurerTests.this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings,
					appSslProperties);
			then(clientBuilderSslSettings).should().tlsCiphers(ciphers);
		}

		@Test
		void multipleCipherOptions() {
			SslConfigProperties appSslProperties = new SslConfigProperties();
			appSslProperties.setEnabled(true);
			appSslProperties.setBundle("foo");
			SslBundle sslBundle = setupJskBundleForBundleName("foo");
			Set<String> ciphers = Set.of("cipher1", "cipher2");
			given(sslBundle.getOptions()).willReturn(SslOptions.of(ciphers, null));
			ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
			ClientBuilderSslConfigurerTests.this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings,
					appSslProperties);
			then(clientBuilderSslSettings).should().tlsCiphers(ciphers);
		}

		@Test
		void singleEnabledProtocolOption() {
			SslConfigProperties appSslProperties = new SslConfigProperties();
			appSslProperties.setEnabled(true);
			appSslProperties.setBundle("foo");
			SslBundle sslBundle = setupJskBundleForBundleName("foo");
			Set<String> protocols = Set.of("protocol1");
			given(sslBundle.getOptions()).willReturn(SslOptions.of(null, protocols));
			ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
			ClientBuilderSslConfigurerTests.this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings,
					appSslProperties);
			then(clientBuilderSslSettings).should().tlsProtocols(protocols);
		}

		@Test
		void multipleEnabledProtocolOptions() {
			SslConfigProperties appSslProperties = new SslConfigProperties();
			appSslProperties.setEnabled(true);
			appSslProperties.setBundle("foo");
			SslBundle sslBundle = setupJskBundleForBundleName("foo");
			Set<String> protocols = Set.of("protocol1", "protocol2");
			given(sslBundle.getOptions()).willReturn(SslOptions.of(null, protocols));
			ClientBuilderSslSettings<?> clientBuilderSslSettings = mock(ClientBuilderSslSettings.class);
			ClientBuilderSslConfigurerTests.this.clientBuilderSslConfigurer.applySsl(clientBuilderSslSettings,
					appSslProperties);
			then(clientBuilderSslSettings).should().tlsProtocols(protocols);
		}

	}

	@Nested
	class SslTypeTests {

		@Test
		void jksType() {
			assertThat(ClientBuilderSslConfigurer.SslType.forSslStoreBundle(new JksSslStoreBundle(null, null)))
				.isEqualTo(SslType.JKS);
		}

		@Test
		void pemType() {
			assertThat(ClientBuilderSslConfigurer.SslType.forSslStoreBundle(new PemSslStoreBundle(null, null)))
				.isEqualTo(SslType.PEM);

		}

		@Test
		void unsupportedType() {
			assertThat(ClientBuilderSslConfigurer.SslType.forSslStoreBundle(SslStoreBundle.NONE))
				.isEqualTo(SslType.UNSUPPORTED);
		}

	}

}
