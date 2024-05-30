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
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.core.env.Environment;

/**
 * Standard {@link EndpointExposerFactory} implementation that checks exposure using an
 * {@link IncludeExcludeEndpointFilter}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.7.22
 */
public abstract class StandardAdditionalEndpointExposerFactory implements EndpointExposerFactory {

	private final String exposureProperty;

	private final String[] defaultIncludes;

	/**
	 * Create a new {@link StandardAdditionalEndpointExposerFactory} instance.
	 * @param id the ID for the exposure. Used to form the + *
	 * {@code management.endpoint.{id}.exposure.include} and
	 * {@code management.enpoint.{id}.exposure.exclude} properties that control exposure
	 * @param defaultIncludes the default include patterns to use when deciding which
	 * endpoints to expose
	 */
	protected StandardAdditionalEndpointExposerFactory(String id, String... defaultIncludes) {
		this.exposureProperty = "management.endpoints." + id + ".exposure";
		this.defaultIncludes = defaultIncludes;
	}

	@Override
	public EndpointExposer getEndpointExposer(Environment environment) {
		return new StandardAdditionalEndpointExposer(environment);
	}

	/**
	 * Determine if this factory supports any of the given {@link EndpointExposure
	 * exposures}.
	 * @param exposures the exposures requested
	 * @return if any of the exposures are supported
	 */
	protected abstract boolean supports(Set<EndpointExposure> exposures);

	/**
	 * Standard {@link EndpointExposer}.
	 */
	private class StandardAdditionalEndpointExposer implements EndpointExposer {

		private final IncludeExcludeEndpointFilter<?> filter;

		StandardAdditionalEndpointExposer(Environment environment) {
			this.filter = new IncludeExcludeEndpointFilter<>(ExposableEndpoint.class, environment,
					StandardAdditionalEndpointExposerFactory.this.exposureProperty,
					StandardAdditionalEndpointExposerFactory.this.defaultIncludes);
		}

		@Override
		public boolean isExposed(EndpointId endpointId, Set<EndpointExposure> exposures) {
			return supports(exposures) && this.filter.match(endpointId);
		}

		@Override
		public String getExposedReason() {
			return "marked as exposed by a '" + StandardAdditionalEndpointExposerFactory.this.exposureProperty
					+ "' property";
		}

	}

}
