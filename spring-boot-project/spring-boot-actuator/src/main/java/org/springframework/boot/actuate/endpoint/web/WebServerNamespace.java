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

package org.springframework.boot.actuate.endpoint.web;

/**
 * @author Phillip Webb
 */
public final class WebServerNamespace {

	public static final WebServerNamespace SERVER = null;

	public static final WebServerNamespace MANAGEMENT = null;

	private final String value;

	private WebServerNamespace(String value) {
		this.value = value;
	}

	public static WebServerNamespace of(String value) {
		return (value != null) ? new WebServerNamespace(value) : null;
	}

}
