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

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Configuration triggered from {@link ManagementWebMvcAutoConfiguration} when a new
 * {@link EmbeddedServletContainer} running on a different port is required.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @see ManagementWebMvcAutoConfiguration
 */
@Configuration
class ManagementWebMvcChildContextConfiguration {

	@Bean
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet dispatcherServlet = new DispatcherServlet();

		// Ensure the parent configuration does not leak down to us
		dispatcherServlet.setDetectAllHandlerMappings(false);
		dispatcherServlet.setDetectAllViewResolvers(false);
		dispatcherServlet.setDetectAllHandlerExceptionResolvers(false);

		// But use the handler adapters from the parent context
		dispatcherServlet.setDetectAllHandlerAdapters(true);

		return dispatcherServlet;
	}

	@Bean
	public HandlerMapping handlerMapping() {
		return new ManagementHandlerMappingAdapter();
	}

	// FIXME reiplement, but should not be an endpoint
	// /*
	// * The error controller is present but not mapped as an endpoint in this context
	// * because of the DispatcherServlet having had it's HandlerMapping explicitly
	// * disabled. So this tiny shim exposes the same feature but only for machine
	// * endpoints.
	// */
	// @Bean
	// public Endpoint<Map<String, Object>> errorEndpoint(final ErrorController
	// controller) {
	// return new AbstractEndpoint<Map<String, Object>>("/error", false, true) {
	// @Override
	// protected Map<String, Object> doInvoke() {
	// RequestAttributes attributes = RequestContextHolder
	// .currentRequestAttributes();
	// return controller.extract(attributes, false);
	// }
	// };
	// }

	// FIXME

	// @Configuration
	// protected static class ServerCustomization implements
	// EmbeddedServletContainerCustomizer {
	//
	// @Value("${error.path:/error}")
	// private String errorPath = "/error";
	//
	// @Autowired
	// private ListableBeanFactory beanFactory;
	//
	// // This needs to be lazily initialized because EmbeddedServletContainerCustomizer
	// // instances get their callback very early in the context lifecycle.
	// private ManagementServerProperties managementServerProperties;
	//
	// @Override
	// public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
	// if (this.managementServerProperties == null) {
	// this.managementServerProperties = BeanFactoryUtils
	// .beanOfTypeIncludingAncestors(this.beanFactory,
	// ManagementServerProperties.class);
	// }
	// factory.setPort(this.managementServerProperties.getPort());
	// factory.setAddress(this.managementServerProperties.getAddress());
	// factory.setContextPath(this.managementServerProperties.getContextPath());
	// factory.addErrorPages(new ErrorPage(this.errorPath));
	// }
	//
	// }
	//
	// /**
	// * Apply security from the parent
	// */
	// @Configuration
	// @ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	// @ConditionalOnBean(name = "springSecurityFilterChain", search =
	// SearchStrategy.PARENTS)
	// public static class EndpointWebMvcChildContextSecurityConfiguration {
	//
	// @Bean
	// public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
	// BeanFactory parent = beanFactory.getParentBeanFactory();
	// return parent.getBean("springSecurityFilterChain", Filter.class);
	// }
	//
	// }

}
