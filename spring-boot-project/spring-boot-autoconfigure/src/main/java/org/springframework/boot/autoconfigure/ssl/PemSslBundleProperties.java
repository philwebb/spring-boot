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

import org.springframework.boot.ssl.pem.PemSslStoreBundle;

/**
 * {@link SslBundleProperties} for PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.1.0
 * @see PemSslStoreBundle
 */
public class PemSslBundleProperties extends SslBundleProperties {

	/**
	 * Keystore properties.
	 */
	private final Store keystore = new Store();

	/**
	 * Truststore properties.
	 */
	private final Store truststore = new Store();

	public Store getKeystore() {
		return this.keystore;
	}

	public Store getTruststore() {
		return this.truststore;
	}

	/**
	 * Store properties.
	 */
	public static class Store {

		/**
		 * Type of the store to create, e.g. JKS.
		 */
		private String type;

		/**
		 * Location or content of the certificate or certificate chain in PEM format.
		 */
		private String certificate;

		/**
		 * Location or content of the private key in PEM format.
		 */
		private String privateKey;

		/**
		 * Password used to decrypt an encrypted private key.
		 */
		private String privateKeyPassword;

		/**
		 * Whether to verify that the private key matches the public key.
		 */
		private boolean verifyKeys;

		private Select select = new Select();

		public String getType() {
			return this.type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getCertificate() {
			return this.certificate;
		}

		public void setCertificate(String certificate) {
			this.certificate = certificate;
		}

		public String getPrivateKey() {
			return this.privateKey;
		}

		public void setPrivateKey(String privateKey) {
			this.privateKey = privateKey;
		}

		public String getPrivateKeyPassword() {
			return this.privateKeyPassword;
		}

		public void setPrivateKeyPassword(String privateKeyPassword) {
			this.privateKeyPassword = privateKeyPassword;
		}

		public boolean isVerifyKeys() {
			return this.verifyKeys;
		}

		public void setVerifyKeys(boolean verifyKeys) {
			this.verifyKeys = verifyKeys;
		}

		public Select getSelect() {
			return this.select;
		}

		/**
		 * Properties used to select certificate and key selection when multiple
		 * candidates exist.
		 */
		public static class Select {

			/**
			 * Which selection method to use when multiple certificate candidates are
			 * found.
			 */
			private SelectCertificate certificate = SelectCertificate.USING_VALIDITY_PERIOD_START;

			/**
			 * Which selection method to use when multiple private key candidates are
			 * found.
			 */
			private SelectPrivateKey privateKey = SelectPrivateKey.USING_FILE_NAME;

			public SelectCertificate getCertificate() {
				return this.certificate;
			}

			public void setCertificate(SelectCertificate certificate) {
				this.certificate = certificate;
			}

			public SelectPrivateKey getPrivateKey() {
				return this.privateKey;
			}

			public void setPrivateKey(SelectPrivateKey privateKey) {
				this.privateKey = privateKey;
			}

			public enum SelectCertificate {

				/**
				 * Select using the latest file creation time of in-date leaf
				 * certificates.
				 */
				USING_FILE_CREATION_TIME(CertificateFileSelector.usingFileCreationTime()),

				/**
				 * Select using the maximum validity period start (the 'not before' field)
				 * of in-date leaf certificates. This is usually the most recently created
				 * certificate.
				 */
				USING_VALIDITY_PERIOD_START(CertificateFileSelector.usingLeafCertificateValidityPeriodStart()),

				/**
				 * Select using the maximum validity period end (the 'not after' field) of
				 * in-date leaf certificates. This is usually the longest usable
				 * certificate.
				 */
				USING_VALIDITY_PERIOD_END(CertificateFileSelector.usingLeafCertificateValidityPeriodEnd()),

				/**
				 * Expect that a certificate file selector bean will select the
				 * certificate.
				 */
				USING_BEAN_SELECTOR((candidates) -> null);

				private final CertificateFileSelector selector;

				SelectCertificate(CertificateFileSelector selector) {
					this.selector = selector.forInDateLeafCertificates();
				}

				CertificateFileSelector getSelector() {
					return this.selector;
				}

			}

			public enum SelectPrivateKey {

				/**
				 * Select using the file with the same basename as the certificate file.
				 */
				USING_FILE_NAME(PrivateKeyFileSelector.usingFileName()),

				/**
				 * Select using the private key that matches the selected certificate.
				 */
				USING_CERTIFICATE_MATCH(PrivateKeyFileSelector.usingCertificateMatch()),

				/**
				 * Expect that a private key file selector bean will select the
				 * certificate.
				 */
				USING_BEAN_SELECTOR((selectedCertificateFile, candidates) -> null);

				private final PrivateKeyFileSelector selector;

				SelectPrivateKey(PrivateKeyFileSelector selector) {
					this.selector = selector;
				}

				PrivateKeyFileSelector getSelector() {
					return this.selector;
				}

			}

		}

	}

}
