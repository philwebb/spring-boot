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

package org.springframework.boot.ssl.pem;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.pem.PemSslStoreDetails.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemSslStoreDetails}.
 *
 * @author Moritz Halbritter
 */
class PemSslStoreDetailsTests {

	@Test
	void pemContent() {
		String content = """
				-----BEGIN CERTIFICATE-----
				MIICpDCCAYwCCQCDOqHKPjAhCTANBgkqhkiG9w0BAQUFADAUMRIwEAYDVQQDDAls
				b2NhbGhvc3QwHhcNMTQwOTEwMjE0MzA1WhcNMTQxMDEwMjE0MzA1WjAUMRIwEAYD
				VQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDR
				0KfxUw7MF/8RB5/YXOM7yLnoHYb/M/6dyoulMbtEdKKhQhU28o5FiDkHcEG9PJQL
				gqrRgAjl3VmCC9omtfZJQ2EpfkTttkJjnKOOroXhYE51/CYSckapBYCVh8GkjUEJ
				uEfnp07cTfYZFqViIgIWPZyjkzl3w4girS7kCuzNdDntVJVx5F/EsFwMA8n3C0Qa
				zHQoM5s00Fer6aTwd6AW0JD5QkADavpfzZ554e4HrVGwHlM28WKQQkFzzGu44FFX
				yVuEF3HeyVPug8GRHAc8UU7ijVgJB5TmbvRGYowIErD5i4VvGLuOv9mgR3aVyN0S
				dJ1N7aJnXpeSQjAgf03jAgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAE4yvwhbPldg
				Bpl7sBw/m2B3bfiNeSqa4tII1PQ7ysgWVb9HbFNKkriScwDWlqo6ljZfJ+SDFCoj
				bQz4fOFdMAOzRnpTrG2NAKMoJLY0/g/p7XO00PiC8T3h3BOJ5SHuW3gUyfGXmAYs
				DnJxJOrwPzj57xvNXjNSbDOJ3DRfCbB0CWBexOeGDiUokoEq3Gnz04Q4ZfHyAcpZ
				3deMw8Od5p9WAoCh3oClpFyOSzXYKZd+3ppMMtfc4wnbfocnfSFxj0UCpOEJw4Ez
				+lGuHKdhNOVW9CmqPD1y76o6c8PQKuF7KZEoY2jvy3GeIfddBvqXgZ4PbWvFz1jO
				32C9XWHwRA4=
				-----END CERTIFICATE-----""";
		PemSslStoreDetails details = new PemSslStoreDetails("JKS", content, content);
		assertThat(details.getCertificateType()).isEqualTo(Type.PEM);
		assertThat(details.getPrivateKeyType()).isEqualTo(Type.PEM);
	}

	@Test
	void location() {
		PemSslStoreDetails details = new PemSslStoreDetails("JKS", "classpath:certificate.pem", "file:privatekey.pem");
		assertThat(details.getCertificateType()).isEqualTo(Type.URL);
		assertThat(details.getPrivateKeyType()).isEqualTo(Type.URL);
	}

	@Test
	void empty() {
		PemSslStoreDetails details = new PemSslStoreDetails(null, null, null);
		assertThat(details.getCertificateType()).isEqualTo(Type.URL);
		assertThat(details.getPrivateKeyType()).isEqualTo(Type.URL);
		assertThat(details.isEmpty()).isTrue();
	}

}
