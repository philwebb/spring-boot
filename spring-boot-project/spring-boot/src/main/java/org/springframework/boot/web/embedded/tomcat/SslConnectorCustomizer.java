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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslKeyReference;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Cyril Dangerville
 */
class SslConnectorCustomizer implements TomcatConnectorCustomizer {

	private final ClientAuth clientAuth;

	private final SslBundle sslBundle;

	SslConnectorCustomizer(ClientAuth clientAuth, SslBundle sslBundle) {
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
	}

	@Override
	public void customize(Connector connector) {
		ProtocolHandler handler = connector.getProtocolHandler();
		Assert.state(handler instanceof AbstractHttp11JsseProtocol,
				"To use SSL, the connector's protocol handler must be an AbstractHttp11JsseProtocol subclass");
		configureSsl((AbstractHttp11JsseProtocol<?>) handler);
		connector.setScheme("https");
		connector.setSecure(true);
	}

	/**
	 * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
	 * @param protocol the protocol
	 */
	void configureSsl(AbstractHttp11JsseProtocol<?> protocol) {
		SslKeyReference key = this.sslBundle.getKey();
		SslStoreBundle stores = this.sslBundle.getStores();
		SslOptions options = this.sslBundle.getOptions();
		protocol.setSSLEnabled(true);
		SSLHostConfig sslHostConfig = new SSLHostConfig();
		sslHostConfig.setHostName(protocol.getDefaultSSLHostConfigName());
		sslHostConfig.setSslProtocol(this.sslBundle.getProtocol());
		protocol.addSslHostConfig(sslHostConfig);
		configureSslClientAuth(sslHostConfig);
		SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
		if (stores.getKeyStorePassword() != null) {
			certificate.setCertificateKeystorePassword(stores.getKeyStorePassword());
		}
		if (key.getPassword() != null) {
			certificate.setCertificateKeyPassword(key.getPassword());
		}
		if (key.getAlias() != null) {
			certificate.setCertificateKeyAlias(key.getAlias());
		}
		sslHostConfig.addCertificate(certificate);
		if (!CollectionUtils.isEmpty(options.getCiphers())) {
			String ciphers = StringUtils.collectionToCommaDelimitedString(options.getCiphers());
			sslHostConfig.setCiphers(ciphers);
		}
		configureEnabledProtocols(protocol);
		configureSslStoreProvider(protocol, sslHostConfig, certificate);
		String keyPassword = key.getPassword();
		if (keyPassword != null) {
			certificate.setCertificateKeyPassword(keyPassword);
		}
	}

	private void configureEnabledProtocols(AbstractHttp11JsseProtocol<?> protocol) {
		SslOptions options = this.sslBundle.getOptions();
		if (!CollectionUtils.isEmpty(options.getEnabledProtocols())) {
			for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
				sslHostConfig.setProtocols(StringUtils.collectionToCommaDelimitedString(options.getEnabledProtocols()));
			}
		}
	}

	private void configureSslClientAuth(SSLHostConfig config) {
		switch (this.clientAuth) {
			case NEED -> config.setCertificateVerification("required");
			case WANT -> config.setCertificateVerification("optional");
			case NONE -> {
			}
		}
	}

	protected void configureSslStoreProvider(AbstractHttp11JsseProtocol<?> protocol, SSLHostConfig sslHostConfig,
			SSLHostConfigCertificate certificate) {
		Assert.isInstanceOf(Http11NioProtocol.class, protocol,
				"SslStoreProvider can only be used with Http11NioProtocol");
		try {
			SslStoreBundle stores = this.sslBundle.getStores();
			if (stores.getKeyStore() != null) {
				certificate.setCertificateKeystore(stores.getKeyStore());
			}
			if (stores.getTrustStore() != null) {
				sslHostConfig.setTrustStore(stores.getTrustStore());
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store: " + ex.getMessage(), ex);
		}
	}

}
