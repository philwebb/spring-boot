/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.Set;

/**
 * Strategy used to get all {@link HealthEndpointGroup}s that have an additional path
 * configured.
 *
 * @author Madhura Bhave
 */
public interface HealthEndpointGroupsWithAdditionalPath {

	// FIXME can we drop and move into HealthEndpointGroups?
	// Perhaps HealthEndpointGroups.get(HealthEndpointGroupAdditionalPath additionalPath)

	HealthEndpointGroupsWithAdditionalPath EMPTY = (prefix) -> Collections.emptySet();

	Set<HealthEndpointGroup> getAll(String prefix);

}
