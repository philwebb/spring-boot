/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.reload.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.omg.CORBA.Environment;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link ApplicationListener} to add sensible refresh properties to the Spring
 * {@link Environment}.
 *
 * @author Phillip Webb
 */
public class RefreshPropertiesApplicationListener implements
		ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final Map<String, Object> PROPERTIES;
	static {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("spring.thymeleaf.cache", "false");
		PROPERTIES = Collections.unmodifiableMap(properties);
	}

	@Override
	public int getOrder() {
		// Order after application.properties have been processed
		return ConfigFileApplicationListener.DEFAULT_ORDER + 10;
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		PropertySource<?> propertySource = new MapPropertySource("refresh", PROPERTIES);
		event.getEnvironment().getPropertySources().addFirst(propertySource);
	}

}
