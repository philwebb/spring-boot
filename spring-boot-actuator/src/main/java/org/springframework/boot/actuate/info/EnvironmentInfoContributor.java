/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.info;

import java.util.Map;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An {@link InfoContributor} that provides all environment entries prefixed with info.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class EnvironmentInfoContributor implements InfoContributor {

	private static final ResolvableType STRING_OBJECT_MAP = ResolvableType
			.forClassWithGenerics(Map.class, String.class, Object.class);

	private final ConfigurableEnvironment environment;

	public EnvironmentInfoContributor(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@Override
	public void contribute(Info.Builder builder) {
		Map<String, Object> info = Binder.get(this.environment).bind("info",
				Bindable.of(STRING_OBJECT_MAP));
		if (info != null) {
			builder.withDetails(info);
		}
	}

}
