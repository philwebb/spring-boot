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

import java.util.Set;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;

/**
 * Strategy used to determine if an endpoint is exposed.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.7.22
 * @see EndpointExposerFactory
 * @see StandardAdditionalEndpointExposerFactory
 */
public interface EndpointExposer {

	/**
	 * Return if the given endpoint is exposed for the given set of exposure technologies.
	 * @param endpointId the endpoint ID
	 * @param exposures the exposure technologies to check
	 * @return if the endpoint is exposed
	 */
	boolean isExposed(EndpointId endpointId, Set<EndpointExposure> exposures);

	/**
	 * Return a the reason that the endpoint was exposed. This will be added to the
	 * {@link ConditionMessage}.
	 * @return the reason the endpoint is exposed
	 */
	String getExposedReason();

}
