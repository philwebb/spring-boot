/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.boot.actuate.web.ManagementHandlerMapping;
import org.springframework.boot.actuate.web.ManagementHandlerExecutionChain;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Adapter class to expose {@link ManagementHandlerMapping}s via {@link HandlerMapping}.
 * 
 * @author Phillip Webb
 */
class ManagementHandlerMappingAdapter implements HandlerMapping, ApplicationContextAware {

	@Autowired(required = false)
	private ManagementServerProperties properties = new ManagementServerProperties();

	private ApplicationContext context;

	private List<ManagementHandlerMapping> handlerMappings;

	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		// FIXME check context path if not in subcontxt
		if (this.handlerMappings == null) {
			this.handlerMappings = findHandlerMappingBeans();
		}
		for (ManagementHandlerMapping handlerMapping : this.handlerMappings) {
			ManagementHandlerExecutionChain handler = handlerMapping.getHandler(this.properties,
					request);
			if (handler != null) {
				return handler.getExecutionChain();
			}
		}
		return null;
	}

	private List<ManagementHandlerMapping> findHandlerMappingBeans() {
		List<ManagementHandlerMapping> mappings = new ArrayList<ManagementHandlerMapping>();
		Map<String, ManagementHandlerMapping> beans = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(this.context,
						ManagementHandlerMapping.class, true, false);
		mappings.addAll(beans.values());
		OrderComparator.sort(mappings);
		return mappings;
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
	}
}
