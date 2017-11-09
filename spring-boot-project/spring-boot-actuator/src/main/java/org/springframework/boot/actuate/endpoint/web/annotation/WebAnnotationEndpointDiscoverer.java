/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.endpoint.ParameterMapper;
import org.springframework.boot.actuate.endpoint.annotation.AnnotationEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.context.ApplicationContext;

/**
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link EndpointWebExtension web extensions} applied to them.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WebAnnotationEndpointDiscoverer
		extends AnnotationEndpointDiscoverer<OperationRequestPredicate, WebOperation> {

	/**
	 * Creates a new {@link WebAnnotationEndpointDiscoverer} that will discover
	 * {@link Endpoint endpoints} and {@link EndpointWebExtension web extensions} using
	 * the given {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @param parameterMapper the {@link ParameterMapper} used to convert arguments when
	 * an operation is invoked
	 * @param cachingConfigurationFactory the {@link CachingConfiguration} factory to use
	 * @param endpointMediaTypes the media types produced and consumed by web endpoint
	 * operations
	 * @param endpointPathResolver the {@link EndpointPathResolver} used to resolve
	 * endpoint paths
	 */
	public WebAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			ParameterMapper parameterMapper,
			CachingConfigurationFactory cachingConfigurationFactory,
			EndpointMediaTypes endpointMediaTypes,
			EndpointPathResolver endpointPathResolver) {
		super(applicationContext,
				new WebEndpointOperationFactory(parameterMapper, endpointMediaTypes,
						endpointPathResolver),
				WebOperation::getRequestPredicate, cachingConfigurationFactory);
	}

	@Override
	protected void verify(Collection<DiscoveredEndpoint> exposedEndpoints) {
		List<List<WebOperation>> clashes = new ArrayList<>();
		exposedEndpoints.forEach((descriptor) -> clashes
				.addAll(descriptor.findDuplicateOperations().values()));
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"Found multiple web operations with matching request predicates:%n"));
			clashes.forEach((clash) -> {
				message.append(" ").append(clash.get(0).getRequestPredicate())
						.append(String.format(":%n"));
				clash.forEach((operation) -> message.append(" ")
						.append(String.format("%s%n", operation)));
			});
			throw new IllegalStateException(message.toString());
		}
	}

}
