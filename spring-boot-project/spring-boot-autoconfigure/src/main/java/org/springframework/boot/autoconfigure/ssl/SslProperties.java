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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.ssl.certificate.CertificateFileSslDetails;
import org.springframework.boot.sslx.keystore.JavaKeyStoreSslDetails;

/**
 * Properties for centralized SSL trust material configuration.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@ConfigurationProperties(prefix = "spring.ssl")
public class SslProperties {

	/**
	 * PEM-encoded SSL trust material.
	 */
	@NestedConfigurationProperty
	private final Map<String, CertificateFileSslDetails> certificate = new LinkedHashMap<>();

	/**
	 * Java keystore SSL trust material.
	 */
	@NestedConfigurationProperty
	private final Map<String, JavaKeyStoreSslDetails> keystore = new LinkedHashMap<>();

	public Map<String, CertificateFileSslDetails> getCertificate() {
		return this.certificate;
	}

	public Map<String, JavaKeyStoreSslDetails> getKeystore() {
		return this.keystore;
	}

}
