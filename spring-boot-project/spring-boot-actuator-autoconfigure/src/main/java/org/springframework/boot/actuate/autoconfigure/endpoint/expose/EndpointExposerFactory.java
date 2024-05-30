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

package org.springframework.boot.actuate.autoconfigure.endpoint.expose;

import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * Factory allowing for additional endpoint exposure strategies to be implemented.
 * Instances should be added to a {@code spring.factories} file to be loaded by the
 * {@link SpringFactoriesLoader}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.7.22
 * @see EndpointExposer
 * @see StandardAdditionalEndpointExposerFactory
 */
public interface EndpointExposerFactory {

	/**
	 * Return an {@link EndpointExposer} instance or {@code null} if the no exposer is
	 * supported in the given environment.
	 * @param environment the environment
	 * @return an {@link EndpointExposer} instance or {@code null}
	 */
	EndpointExposer getEndpointExposer(Environment environment);

}
