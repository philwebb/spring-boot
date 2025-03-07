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

package org.springframework.boot.web.client;

import org.springframework.boot.selector.SelectableSet;
import org.springframework.web.client.RestClient;

/**
 * A {@link SelectableSet} of {@link RestClient rest clients}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface RestClients extends SelectableSet<RestClients, RestClient> {

	/**
	 * Return the builders that were used to create the rest clients in this set.
	 * @return the rest client builders
	 */
	RestClientBuilders builders();

	/**
	 * Factory method to create a new {@link RestClients} instance using the given
	 * builders.
	 * @param restClientBuilders the rest client builders used to create this set
	 * @return a new {@link RestClients} instance.
	 */
	static RestClients fromBuilders(RestClientBuilders restClientBuilders) {
		return new SimpleRestClients(restClientBuilders);
	}

}
