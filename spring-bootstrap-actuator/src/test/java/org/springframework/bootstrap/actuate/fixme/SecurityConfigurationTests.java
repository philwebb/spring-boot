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

package org.springframework.bootstrap.actuate.fixme;


/**
 * @author Dave Syer
 */
public class SecurityConfigurationTests {

	// private AnnotationConfigWebApplicationContext context;
	//
	// @Test
	// public void testWebConfiguration() throws Exception {
	// this.context = new AnnotationConfigWebApplicationContext();
	// this.context.setServletContext(new MockServletContext());
	// this.context.register(SecurityAutoConfiguration.class, EndpointsProperties.class,
	// PropertyPlaceholderAutoConfiguration.class);
	// this.context.refresh();
	// assertNotNull(this.context.getBean(AuthenticationManager.class));
	// }
	//
	// @Test
	// public void testOverrideAuthenticationManager() throws Exception {
	// this.context = new AnnotationConfigWebApplicationContext();
	// this.context.setServletContext(new MockServletContext());
	// this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
	// EndpointsProperties.class, PropertyPlaceholderAutoConfiguration.class);
	// this.context.refresh();
	// assertEquals(this.context.getBean(TestConfiguration.class).authenticationManager,
	// this.context.getBean(AuthenticationManager.class));
	// }
	//
	// @Configuration
	// protected static class TestConfiguration {
	//
	// private AuthenticationManager authenticationManager;
	//
	// @Bean
	// public AuthenticationManager myAuthenticationManager() {
	// this.authenticationManager = new AuthenticationManager() {
	//
	// @Override
	// public Authentication authenticate(Authentication authentication)
	// throws AuthenticationException {
	// return new TestingAuthenticationToken("foo", "bar");
	// }
	// };
	// return this.authenticationManager;
	// }
	//
	// }

}
