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

package org.springframework.boot.web.embedded.undertow;

import java.net.InetAddress;

import javax.net.ssl.SSLContext;

import io.undertow.Undertow;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.SslClientAuthMode;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslDetails;
import org.springframework.boot.web.server.Ssl.ClientAuth;

/**
 * {@link UndertowBuilderCustomizer} that configures SSL on the given builder instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Cyril Dangerville
 * @author Scott Frederick
 */
class SslBuilderCustomizer implements UndertowBuilderCustomizer {

	private final int port;

	private final InetAddress address;

	private final ClientAuth clientAuth;

	private final SslBundle sslBundle;

	SslBuilderCustomizer(int port, InetAddress address, ClientAuth clientAuth, SslBundle sslBundle) {
		this.port = port;
		this.address = address;
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
	}

	@Override
	public void customize(Undertow.Builder builder) {
		SSLContext sslContext = this.sslBundle.getSslContext();
		builder.addHttpsListener(this.port, getListenAddress(), sslContext);
		SslDetails ssl = this.sslBundle.getDetails();
		builder.setSocketOption(Options.SSL_CLIENT_AUTH_MODE, getSslClientAuthMode(this.clientAuth));
		if (ssl.getEnabledProtocols() != null) {
			builder.setSocketOption(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(ssl.getEnabledProtocols()));
		}
		if (ssl.getCiphers() != null) {
			builder.setSocketOption(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(ssl.getCiphers()));
		}
	}

	private String getListenAddress() {
		if (this.address == null) {
			return "0.0.0.0";
		}
		return this.address.getHostAddress();
	}

	private SslClientAuthMode getSslClientAuthMode(ClientAuth clientAuth) {
		if (clientAuth == ClientAuth.NEED) {
			return SslClientAuthMode.REQUIRED;
		}
		if (clientAuth == ClientAuth.WANT) {
			return SslClientAuthMode.REQUESTED;
		}
		return SslClientAuthMode.NOT_REQUESTED;
	}

}
