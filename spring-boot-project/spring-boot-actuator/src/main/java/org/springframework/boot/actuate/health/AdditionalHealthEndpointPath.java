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

import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;

/**
 * @author Phillip Webb
 * @since 2.6.0
 */
public class AdditionalHealthEndpointPath {

	/**
	 * @return
	 */
	public String getPath() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	// FIXME thing to do the split / parse logic
	// probably got ServerNamespace and String[] parts

	/**
	 * @param webServerNamespace
	 * @param path
	 * @return
	 */
	public static AdditionalHealthEndpointPath of(WebServerNamespace webServerNamespace, String[] path) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public static AdditionalHealthEndpointPath parse(String value) {
		return null;
	}

	/**
	 * @param namespace
	 * @return
	 */
	public boolean hasNamespace(WebServerNamespace namespace) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
