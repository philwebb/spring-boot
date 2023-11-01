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

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties.Store.Select.SelectCertificate;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties.Store.Select.SelectPrivateKey;
import org.springframework.boot.ssl.pem.PemContent;
import org.springframework.boot.ssl.pem.PemSslStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultPemSslStoreFactory}.
 *
 * @author Phillip Webb
 */
class DefaultPemSslStoreFactoryTests {

	@Captor
	private ArgumentCaptor<List<CertificateFile>> certificateFilesCaptor;

	@Captor
	private ArgumentCaptor<CertificateFile> certificateFileCaptor;

	@Captor
	private ArgumentCaptor<List<PrivateKeyFile>> privateKeyFilesCaptor;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void getPemSslStoreWhenCertificateIsNotDirectoryGlobAndPrivateKeyIsDirectoryGlobThrowsException() {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate("file.pem");
		properties.setPrivateKey("*.key");
		assertThatIllegalStateException().isThrownBy(() -> factory.getPemSslStore("bundle", "test", properties))
			.withMessage("Property 'test.private-key' cannot contain a directory glob pattern");
	}

	@Test
	void getPemSslStoreWhenCertificateIsNotDirectoryGlobAndPrivateKeyIsNotDirectoryGlobReturnsPemSslStore()
			throws Exception {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("rsa-cert.pem"));
		properties.setPrivateKey(testResource("rsa-key.pem"));
		PemSslStore store = factory.getPemSslStore("bundle", "test", properties);
		assertThat(store.certificates()).isEqualTo(loadCertificates("rsa-cert.pem"));
		assertThat(store.privateKey()).isEqualTo(loadPrivateKey("rsa-key.pem"));
	}

	@Test
	void getPemSslStoreWhenCertificateIsDirectoryGlobAndPrivateKeyIsNotDirectoryGlobThrowsException() {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		properties.setPrivateKey(testResource("key2.pem"));
		assertThatIllegalStateException().isThrownBy(() -> factory.getPemSslStore("bundle", "test", properties))
			.withMessage("Property 'test.private-key' must contain a directory glob pattern");
	}

	@Test
	void getPemSslStoreWhenUsingDirectoryGlobsAndSelectorsMatchCreatesPemSslStoreWithSelectorSelection()
			throws Exception {
		List<CertificateFileSelector> certificateFileSelectors = new ArrayList<>();
		certificateFileSelectors.add(mock(CertificateFileSelector.class));
		certificateFileSelectors.add((candidates) -> candidates.stream()
			.filter((candidate) -> candidate.path().getFileName().toString().contains("2."))
			.findFirst()
			.orElseThrow());
		List<PrivateKeyFileSelector> privateKeyFileSelectors = new ArrayList<>();
		privateKeyFileSelectors.add(mock(PrivateKeyFileSelector.class));
		privateKeyFileSelectors.add(PrivateKeyFileSelector.usingFileName());
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory(certificateFileSelectors,
				privateKeyFileSelectors);
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		properties.setPrivateKey(testResource("key*.pem"));
		PemSslStore store = factory.getPemSslStore("bundle", "test", properties);
		assertThat(store.certificates()).isEqualTo(loadCertificates("key2.crt"));
		assertThat(store.privateKey()).isEqualTo(loadPrivateKey("key2.pem"));
		then(certificateFileSelectors.get(0)).should()
			.selectCertificateFile(eq("bundle"), this.certificateFilesCaptor.capture());
		assertThat(this.certificateFilesCaptor.getValue()
			.stream()
			.map((certificateFile) -> certificateFile.path().getFileName().toString()))
			.contains("key1.crt", "key2.crt");
		then(privateKeyFileSelectors.get(0)).should()
			.selectPrivateKeyFile(eq("bundle"), this.certificateFileCaptor.capture(),
					this.privateKeyFilesCaptor.capture());
		assertThat(this.certificateFileCaptor.getValue().path().getFileName()).hasToString("key2.crt");
		assertThat(this.privateKeyFilesCaptor.getValue()
			.stream()
			.map((certificateFile) -> certificateFile.path().getFileName().toString()))
			.contains("key1.pem", "key2.pem");
	}

	@Test
	void getPemSslStoreWhenUsingDirectoryGlobsAndNoSelectorsMatchCreatesPemSslStoreWithPropertySelection()
			throws Exception {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		properties.setPrivateKey(testResource("key*.pem"));
		properties.getSelect().setPrivateKey(SelectPrivateKey.USING_CERTIFICATE_MATCH);
		PemSslStore store = factory.getPemSslStore("bundle", "test", properties);
		assertThat(store.certificates()).isEqualTo(loadCertificates("key2-chain.crt"));
		assertThat(store.privateKey()).isEqualTo(loadPrivateKey("key2.pem"));
	}

	@Test
	void getPemSslStoreWhenUsingCertificateDirectoryGlobsAndNoPrivateKeyCreatesPemSslStore() throws IOException {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		PemSslStore store = factory.getPemSslStore("bundle", "test", properties);
		assertThat(store.certificates()).isEqualTo(loadCertificates("key2-chain.crt"));
		assertThat(store.privateKey()).isNull();
	}

	@Test
	void getPemSslStoreWhenNoCertificateSelectedThrowsException() {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		properties.getSelect().setCertificate(SelectCertificate.USING_BEAN_SELECTOR);
		assertThatIllegalStateException().isThrownBy(() -> factory.getPemSslStore("bundle", "test", properties))
			.withMessageContaining("No certificate file selected");
	}

	@Test
	void getPemSslStoreWhenNoPrivateKeySelectedThrowsException() {
		DefaultPemSslStoreFactory factory = new DefaultPemSslStoreFactory();
		PemSslBundleProperties.Store properties = new PemSslBundleProperties.Store();
		properties.setCertificate(testResource("key*.crt"));
		properties.setPrivateKey(testResource("key*.pem"));
		properties.getSelect().setPrivateKey(SelectPrivateKey.USING_BEAN_SELECTOR);
		assertThatIllegalStateException().isThrownBy(() -> factory.getPemSslStore("bundle", "test", properties))
			.withMessageContaining("No private key file selected");
	}

	private List<X509Certificate> loadCertificates(String name) throws IOException {
		return PemContent.load(getClass().getResource(name)).getCertificates();
	}

	private PrivateKey loadPrivateKey(String name) throws IOException {
		return PemContent.load(getClass().getResource(name)).getPrivateKey();
	}

	private static String testResource(String name) {
		return "src/test/resources/org/springframework/boot/autoconfigure/ssl/" + name;
	}

}
