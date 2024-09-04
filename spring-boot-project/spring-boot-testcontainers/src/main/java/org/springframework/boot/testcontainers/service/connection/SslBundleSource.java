/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;

/**
 * {@link SslBundle} source created from annotations. Used as a cache key and as a
 * {@link SslBundle} factory.
 *
 * @param ssl the {@link Ssl @Ssl} annotation
 * @param pemKeyStore the {@link PemKeyStore @PemKeyStore} annotation
 * @param pemTrustStore the {@link PemTrustStore @PemTrustStore} annotation
 * @param jksKeyStore the {@link JksKeyStore @JksKeyStore} annotation
 * @param jksTrustStore the {@link JksTrustStore @JksTrustStore} annotation
 * @author Phillip Webb
 */
record SslBundleSource(Ssl ssl, PemKeyStore pemKeyStore, PemTrustStore pemTrustStore, JksKeyStore jksKeyStore,
		JksTrustStore jksTrustStore) {

	SslBundleSource {
		ssl = (ssl != null) ? ssl : MergedAnnotation.of(Ssl.class).synthesize();
		boolean hasPem = (pemKeyStore != null || pemTrustStore != null);
		boolean hasJks = (jksKeyStore != null || jksTrustStore != null);
		Assert.state((!hasPem && !hasJks) || (hasPem && !hasJks) || (!hasPem && hasJks),
				"PEM and JKS store annotations cannot be used together");
	}

	SslBundle getSslBundle() {
		SslStoreBundle stores = stores();
		SslOptions options = SslOptions.of(this.ssl.ciphers(), this.ssl.enabledProtocols());
		SslBundleKey key = SslBundleKey.of(this.ssl.keyPassword(), this.ssl.keyAlias());
		String protocol = this.ssl.protocol();
		return SslBundle.of(stores, key, options, protocol);
	}

	private SslStoreBundle stores() {
		if (this.pemKeyStore != null || this.pemTrustStore != null) {
			return new PemSslStoreBundle(pemKeyStoreDetails(), pemTrustStoreDetails());
		}
		if (this.jksKeyStore != null || this.jksTrustStore != null) {
			return new JksSslStoreBundle(jksKeyStoreDetails(), jksTrustStoreDetails());
		}
		return null;
	}

	private PemSslStoreDetails pemKeyStoreDetails() {
		PemKeyStore store = this.pemKeyStore;
		return (store != null) ? new PemSslStoreDetails(store.type(), store.certificate(), store.privateKey(),
				store.privateKeyPassword()) : null;
	}

	private PemSslStoreDetails pemTrustStoreDetails() {
		PemTrustStore store = this.pemTrustStore;
		return (store != null) ? new PemSslStoreDetails(store.type(), store.certificate(), store.privateKey(),
				store.privateKeyPassword()) : null;
	}

	private JksSslStoreDetails jksTrustStoreDetails() {
		JksKeyStore store = this.jksKeyStore;
		return (store != null)
				? new JksSslStoreDetails(store.type(), store.provider(), store.location(), store.password()) : null;
	}

	private JksSslStoreDetails jksKeyStoreDetails() {
		JksTrustStore store = this.jksTrustStore;
		return (store != null)
				? new JksSslStoreDetails(store.type(), store.provider(), store.location(), store.password()) : null;
	}

	static SslBundleSource get(MergedAnnotations annotations) {
		return get(null, null, annotations);
	}

	static SslBundleSource get(ConfigurableListableBeanFactory beanFactory, String beanName,
			MergedAnnotations annotations) {
		Ssl ssl = getAnnotation(beanFactory, beanName, annotations, Ssl.class);
		PemKeyStore pemKeyStore = getAnnotation(beanFactory, beanName, annotations, PemKeyStore.class);
		PemTrustStore pemTrustStore = getAnnotation(beanFactory, beanName, annotations, PemTrustStore.class);
		JksKeyStore jksKeyStore = getAnnotation(beanFactory, beanName, annotations, JksKeyStore.class);
		JksTrustStore jksTrustStore = getAnnotation(beanFactory, beanName, annotations, JksTrustStore.class);
		if (ssl == null && pemKeyStore == null && pemTrustStore == null && jksKeyStore == null
				&& jksTrustStore == null) {
			return null;
		}
		return new SslBundleSource(ssl, pemKeyStore, pemTrustStore, jksKeyStore, jksTrustStore);
	}

	private static <A extends Annotation> A getAnnotation(ConfigurableListableBeanFactory beanFactory, String beanName,
			MergedAnnotations annotations, Class<A> annotationType) {
		Set<A> found = (beanFactory != null) ? beanFactory.findAllAnnotationsOnBean(beanName, annotationType, false)
				: Collections.emptySet();
		if (annotations != null) {
			found = new LinkedHashSet<>(found);
			annotations.stream(annotationType).map(MergedAnnotation::synthesize).forEach(found::add);
		}
		Assert.state(found.size() <= 1,
				() -> "Unable to find single %s annotation".formatted(annotationType.getName()));
		return (!found.isEmpty()) ? found.iterator().next() : null;
	}

}
