/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.web.servlet;

import org.springframework.boot.autoconfigure.DeprecatedAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Deprecated {@link EnableAutoConfiguration auto-configuration} for Spring Security
 * available for back compatibilty.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Rob Winch
 * @since 2.1.0
 */
@DeprecatedAutoConfiguration(replacement = {
		"org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration",
		"org.springframework.boot.autoconfigure.security.web.servlet.ServletWebSecurityAutoConfiguration" })
public class SecurityAutoConfiguration {

}
