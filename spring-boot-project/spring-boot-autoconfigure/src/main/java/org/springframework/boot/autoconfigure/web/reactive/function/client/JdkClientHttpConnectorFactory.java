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

import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;

/**
 * {@link ClientHttpConnectorFactory} for {@link JettyClientHttpConnector}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpConnectorFactory implements ClientHttpConnectorFactory<JdkClientHttpConnector> {

	@Override
	public JdkClientHttpConnector createClientHttpConnector() {
		return new JdkClientHttpConnector();
	}

}
