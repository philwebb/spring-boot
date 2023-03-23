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

package org.springframework.boot.web.embedded.jetty;

import java.net.InetSocketAddress;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.SslConfigurationValidator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link JettyServerCustomizer} that configures SSL on the given Jetty server instance.
 *
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chris Bono
 * @author Cyril Dangerville
 * @author Scott Frederick
 */
class SslServerCustomizer implements JettyServerCustomizer {

	private final InetSocketAddress address;

	private final ClientAuth clientAuth;

	private final SslBundle sslBundle;

	private final Http2 http2;

	SslServerCustomizer(InetSocketAddress address, ClientAuth clientAuth, SslBundle sslBundle, Http2 http2) {
		this.address = address;
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
		this.http2 = http2;
	}

	@Override
	public void customize(Server server) {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setEndpointIdentificationAlgorithm(null);
		configureSsl(sslContextFactory, this.clientAuth, this.sslBundle);
		ServerConnector connector = createConnector(server, sslContextFactory, this.address,
				this.sslBundle.getDetails());
		server.setConnectors(new Connector[] { connector });
	}

	private ServerConnector createConnector(Server server, SslContextFactory.Server sslContextFactory,
			InetSocketAddress address, SslDetails ssl) {
		HttpConfiguration config = new HttpConfiguration();
		config.setSendServerVersion(false);
		config.setSecureScheme("https");
		config.setSecurePort(address.getPort());
		config.addCustomizer(new SecureRequestCustomizer());
		ServerConnector connector = createServerConnector(server, sslContextFactory, config, ssl);
		connector.setPort(address.getPort());
		connector.setHost(address.getHostString());
		return connector;
	}

	private ServerConnector createServerConnector(Server server, SslContextFactory.Server sslContextFactory,
			HttpConfiguration config, SslDetails ssl) {
		if (this.http2 == null || !this.http2.isEnabled()) {
			return createHttp11ServerConnector(server, config, sslContextFactory, ssl);
		}
		Assert.state(isJettyAlpnPresent(),
				() -> "An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.");
		Assert.state(isJettyHttp2Present(),
				() -> "The 'org.eclipse.jetty.http2:http2-server' dependency is required for HTTP/2 support.");
		return createHttp2ServerConnector(server, config, sslContextFactory, ssl);
	}

	private ServerConnector createHttp11ServerConnector(Server server, HttpConfiguration config,
			SslContextFactory.Server sslContextFactory, SslDetails ssl) {
		HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
		return new SslValidatingServerConnector(server, sslContextFactory, ssl.getKeyAlias(),
				createSslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), connectionFactory);
	}

	private SslConnectionFactory createSslConnectionFactory(SslContextFactory.Server sslContextFactory,
			String protocol) {
		try {
			return new SslConnectionFactory(sslContextFactory, protocol);
		}
		catch (NoSuchMethodError ex) {
			// Jetty 10
			try {
				return SslConnectionFactory.class.getConstructor(SslContextFactory.Server.class, String.class)
					.newInstance(sslContextFactory, protocol);
			}
			catch (Exception ex2) {
				throw new RuntimeException(ex2);
			}
		}
	}

	private boolean isJettyAlpnPresent() {
		return ClassUtils.isPresent("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory", null);
	}

	private boolean isJettyHttp2Present() {
		return ClassUtils.isPresent("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory", null);
	}

	private ServerConnector createHttp2ServerConnector(Server server, HttpConfiguration config,
			SslContextFactory.Server sslContextFactory, SslDetails ssl) {
		HttpConnectionFactory http = new HttpConnectionFactory(config);
		HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(config);
		ALPNServerConnectionFactory alpn = createAlpnServerConnectionFactory();
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		if (isConscryptPresent()) {
			sslContextFactory.setProvider("Conscrypt");
		}
		SslConnectionFactory connectionFactory = createSslConnectionFactory(sslContextFactory, alpn.getProtocol());
		return new SslValidatingServerConnector(server, sslContextFactory, ssl.getKeyAlias(), connectionFactory, alpn,
				h2, http);
	}

	private ALPNServerConnectionFactory createAlpnServerConnectionFactory() {
		try {
			return new ALPNServerConnectionFactory();
		}
		catch (IllegalStateException ex) {
			throw new IllegalStateException(
					"An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.", ex);
		}
	}

	private boolean isConscryptPresent() {
		return ClassUtils.isPresent("org.conscrypt.Conscrypt", null)
				&& ClassUtils.isPresent("org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor", null);
	}

	/**
	 * Configure the SSL connection.
	 * @param factory the Jetty {@link Server SslContextFactory.Server}.
	 * @param clientAuth the client authentication mode
	 * @param sslBundle the SSL trust material
	 */
	protected void configureSsl(SslContextFactory.Server factory, ClientAuth clientAuth, SslBundle sslBundle) {
		SslDetails ssl = sslBundle.getDetails();
		factory.setProtocol(ssl.getProtocol());
		configureSslClientAuth(factory, clientAuth);
		configureSslPasswords(factory, sslBundle);
		factory.setCertAlias(ssl.getKeyAlias());
		if (!ObjectUtils.isEmpty(ssl.getCiphers())) {
			factory.setIncludeCipherSuites(ssl.getCiphers());
			factory.setExcludeCipherSuites();
		}
		if (ssl.getEnabledProtocols() != null) {
			factory.setIncludeProtocols(ssl.getEnabledProtocols());
		}
		if (sslBundle != null) {
			try {
				String keyPassword = sslBundle.getKeyStores().getKeyPassword();
				if (keyPassword != null) {
					factory.setKeyManagerPassword(keyPassword);
				}
				factory.setKeyStore(sslBundle.getKeyStores().getKeyStore());
				factory.setTrustStore(sslBundle.getKeyStores().getTrustStore());
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set SSL store: " + ex.getMessage(), ex);
			}
		}
	}

	private void configureSslClientAuth(SslContextFactory.Server factory, ClientAuth clientAuth) {
		if (clientAuth == ClientAuth.NEED) {
			factory.setNeedClientAuth(true);
			factory.setWantClientAuth(true);
		}
		else if (clientAuth == ClientAuth.WANT) {
			factory.setWantClientAuth(true);
		}
	}

	private void configureSslPasswords(SslContextFactory.Server factory, SslBundle sslBundle) {
		if (sslBundle.getKeyStores().getKeyStorePassword() != null) {
			factory.setKeyStorePassword(sslBundle.getKeyStores().getKeyStorePassword());
		}
		if (sslBundle.getKeyStores().getKeyPassword() != null) {
			factory.setKeyManagerPassword(sslBundle.getKeyStores().getKeyPassword());
		}
	}

	/**
	 * A {@link ServerConnector} that validates the ssl key alias on server startup.
	 */
	static class SslValidatingServerConnector extends ServerConnector {

		private final SslContextFactory sslContextFactory;

		private final String keyAlias;

		SslValidatingServerConnector(Server server, SslContextFactory sslContextFactory, String keyAlias,
				SslConnectionFactory sslConnectionFactory, HttpConnectionFactory connectionFactory) {
			super(server, sslConnectionFactory, connectionFactory);
			this.sslContextFactory = sslContextFactory;
			this.keyAlias = keyAlias;
		}

		SslValidatingServerConnector(Server server, SslContextFactory sslContextFactory, String keyAlias,
				ConnectionFactory... factories) {
			super(server, factories);
			this.sslContextFactory = sslContextFactory;
			this.keyAlias = keyAlias;
		}

		@Override
		protected void doStart() throws Exception {
			super.doStart();
			SslConfigurationValidator.validateKeyAlias(this.sslContextFactory.getKeyStore(), this.keyAlias);
		}

	}

}
