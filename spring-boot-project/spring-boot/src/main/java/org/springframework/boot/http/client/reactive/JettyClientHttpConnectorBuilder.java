/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.http.client.reactive;

import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.util.ClassUtils;

/**
 * @author pwebb
 */
public class JettyClientHttpConnectorBuilder extends AbstractClientHttpRequestFactoryBuilder<JettyClientHttpConnector> {

	static class Classes {

		static final String HTTP_CLIENT = "org.eclipse.jetty.client.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
