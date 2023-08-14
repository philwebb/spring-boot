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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AuthParameterUtils}.
 *
 * @author Alexander Preuß
 * @author Chris Bono
 */
class AuthParameterUtilsTests {

	@Test
	void encodedParamStringIsNullWhenAuthParamsMapIsNull() {
		assertThat(AuthParameterUtils.maybeConvertToEncodedParamString(null)).isNull();
	}

	@Test
	void encodedParamStringIsNullWhenAuthParamsMapIsEmpty() {
		assertThat(AuthParameterUtils.maybeConvertToEncodedParamString(Collections.emptyMap())).isNull();
	}

	@Test
	void encodedParamStringOrdersByAuthParamsMapKeys() {
		Map<String, String> authParamsMap = Map.of("issuerUrl", "https://auth.server.cloud", "privateKey",
				"file://Users/xyz/key.json", "audience", "urn:sn:pulsar:abc:xyz");
		String encodedAuthParamString = AuthParameterUtils.maybeConvertToEncodedParamString(authParamsMap);
		assertThat(encodedAuthParamString).isEqualTo("{\"audience\":\"urn:sn:pulsar:abc:xyz\","
				+ "\"issuerUrl\":\"https://auth.server.cloud\",\"privateKey\":\"file://Users/xyz/key.json\"}");
	}

	@Test
	void encodedParamStringDoesNotSupportRelaxedBinding() {
		// kebab-case keys are left alone (eg. 'issuer-url')
		// lowercase keys are left alone (eg. 'privatekey')
		Map<String, String> authParamsMap = Map.of("issuer-url", "https://auth.server.cloud", "privatekey",
				"file://Users/xyz/key.json", "audience", "urn:sn:pulsar:abc:xyz");
		String encodedAuthParamString = AuthParameterUtils.maybeConvertToEncodedParamString(authParamsMap);
		assertThat(encodedAuthParamString).isEqualTo("{\"audience\":\"urn:sn:pulsar:abc:xyz\","
				+ "\"issuer-url\":\"https://auth.server.cloud\",\"privatekey\":\"file://Users/xyz/key.json\"}");
	}

}
