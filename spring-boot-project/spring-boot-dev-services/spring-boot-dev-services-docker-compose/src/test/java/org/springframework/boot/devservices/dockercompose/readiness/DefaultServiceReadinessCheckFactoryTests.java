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

package org.springframework.boot.devservices.dockercompose.readiness;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devservices.dockercompose.configuration.DockerComposeDevServiceConfigurationProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultServiceReadinessCheckFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DefaultServiceReadinessCheckFactoryTests {

	@Test
	void create() {
		MockEnvironment environment = new MockEnvironment();
		DockerComposeDevServiceConfigurationProperties configuration = new DockerComposeDevServiceConfigurationProperties();
		configuration.getReadiness().getTcp().setConnectTimeout(Duration.ofSeconds(1));
		configuration.getReadiness().getTcp().setReadTimeout(Duration.ofSeconds(2));
		ServiceReadinessCheck readinessCheck = new DefaultServiceReadinessCheckFactory().create(configuration);
		assertThat(readinessCheck).isInstanceOf(TcpConnectServiceReadinessCheck.class);
		assertThat(readinessCheck).extracting("connectTimeout").isEqualTo(Duration.ofSeconds(1));
		assertThat(readinessCheck).extracting("readTimeout").isEqualTo(Duration.ofSeconds(2));
	}

}
