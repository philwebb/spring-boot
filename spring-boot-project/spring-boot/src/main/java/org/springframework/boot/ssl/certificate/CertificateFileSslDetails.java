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

package org.springframework.boot.ssl.certificate;

import org.springframework.boot.ssl.SslDetails;

/**
 * SSL configuration with PEM-encoded certificate files.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class CertificateFileSslDetails extends SslDetails {

	/**
	 * The location of the certificate in PEM format.
	 */
	private String certificate;

	/**
	 * The location of the private key for the certificate in PEM format.
	 */
	private String certificatePrivateKey;

	/**
	 * The location of the trust certificate authority chain in PEM format.
	 */
	private String trustCertificate;

	/**
	 * The location of the private key for the trust certificate in PEM format.
	 */
	private String trustCertificatePrivateKey;

	public String getCertificate() {
		return this.certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getCertificatePrivateKey() {
		return this.certificatePrivateKey;
	}

	public void setCertificatePrivateKey(String certificatePrivateKey) {
		this.certificatePrivateKey = certificatePrivateKey;
	}

	public String getTrustCertificate() {
		return this.trustCertificate;
	}

	public void setTrustCertificate(String trustCertificate) {
		this.trustCertificate = trustCertificate;
	}

	public String getTrustCertificatePrivateKey() {
		return this.trustCertificatePrivateKey;
	}

	public void setTrustCertificatePrivateKey(String trustCertificatePrivateKey) {
		this.trustCertificatePrivateKey = trustCertificatePrivateKey;
	}

}
