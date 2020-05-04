/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertyMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class SystemEnvironmentPropertyMapperTests extends AbstractPropertyMapperTests {

	@Override
	protected PropertyMapper getMapper() {
		return SystemEnvironmentPropertyMapper.INSTANCE;
	}

	@Test
	void mapFromStringShouldReturnBestGuess() {
		assertThat(mapPropertySourceName("SERVER")).isEqualTo("server");
		assertThat(mapPropertySourceName("SERVER_PORT")).isEqualTo("server.port");
		assertThat(mapPropertySourceName("HOST_0")).isEqualTo("host[0]");
		assertThat(mapPropertySourceName("HOST_0_1")).isEqualTo("host[0][1]");
		assertThat(mapPropertySourceName("HOST_0_NAME")).isEqualTo("host[0].name");
		assertThat(mapPropertySourceName("HOST_F00_NAME")).isEqualTo("host.f00.name");
		assertThat(mapPropertySourceName("S-ERVER")).isEqualTo("s-erver");
	}

	@Test
	void mapFromConfigurationShouldReturnBestGuess() {
		assertThat(mapConfigurationPropertyName("server")).containsExactly("SERVER");
		assertThat(mapConfigurationPropertyName("server.port")).containsExactly("SERVER_PORT");
		assertThat(mapConfigurationPropertyName("host[0]")).containsExactly("HOST_0");
		assertThat(mapConfigurationPropertyName("host[0][1]")).containsExactly("HOST_0_1");
		assertThat(mapConfigurationPropertyName("host[0].name")).containsExactly("HOST_0_NAME");
		assertThat(mapConfigurationPropertyName("host.f00.name")).containsExactly("HOST_F00_NAME");
		assertThat(mapConfigurationPropertyName("foo.the-bar")).containsExactly("FOO_THEBAR", "FOO_THE_BAR");
	}

	@Test
	@Disabled // FIXME
	void underscoreShouldNotMapToEmptyString() {
		ConfigurationPropertyName maped = getMapper().map("_");
		assertThat(maped).isNotEqualTo(ConfigurationPropertyName.of(""));
	}

	@Test
	@Disabled // FIXME
	void underscoreWithWhitespaceShouldNotMapToEmptyString() {
		// PropertyMapping[] mappings = getMapper().map(" _");
		// boolean applicable = false;
		// for (PropertyMapping mapping : mappings) {
		// applicable = mapping.isApplicable(ConfigurationPropertyName.of(""));
		// }
		// assertThat(applicable).isFalse();
	}

}
