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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

/**
 * @author pwebb
 */
public class Temp {

	/
	Pull these
	in however you like @Value("${user.keystore}")
	String keyStoreLocation;

	@Value("${user.keystore.passwd}")
	String keyStorePassword;

	@Value("${user.truststore}")
	String trustStoreLocation;

	@Value("${user.truststore.passwd}")
	String trustStorePassword;

	...

	HttpClient httpClient=HttpClient.create().secure(spec->{try{KeyStore keyStore=KeyStore.getInstance("JKS");keyStore.load(new FileInputStream(ResourceUtils.getFile(keyStoreLocation)),keyStorePassword.toCharArray());

	KeyManagerFactory keyManagerFactory=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());keyManagerFactory.init(keyStore,keyStorePassword.toCharArray());

	KeyStore trustStore=KeyStore.getInstance("JKS");trustStore.load(new FileInputStream((ResourceUtils.getFile(trustStoreLocation))),trustStorePassword.toCharArray());

	TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trustManagerFactory.init(trustStore);

	spec.sslContext(SslContextBuilder.forClient().keyManager(keyManagerFactory).trustManager(trustManagerFactory).build());

	}catch(
	Exception e)
	{
		logger.error("Unable to set SSL Context", this.e);
	}});

	WebClient webClient = WebClient.builder()
		.baseUrl(baseUrl)
		.clientConnector(new ReactorClientHttpConnector(this.httpClient))
		.build();

}
