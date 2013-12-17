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

package org.springframework.boot.actuate.endpoint;

/**
 * An endpoint that can be used to expose useful information to operations. Usually
 * exposed via Spring MVC or JMX, but could also be exposed using some other technique.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Christian Dupuis
 */
public interface Endpoint<T> {

	/**
	 * Return the path segment for the endpoint. Must be a simple word (i.e. a regex
	 * pattern of '\w').
	 */
	String getPathSegment();

	/**
	 * Return if the endpoint is enabled.
	 */
	public boolean isEnabled();

	/**
	 * Return if the endpoint is sensitive, i.e. may return data that the average user
	 * should not see. Mappings can use this as a security hint.
	 */
	boolean isSensitive();

	/**
	 * Called to invoke the endpoint.
	 * @return the results of the invocation
	 */
	T invoke();

	/**
	 * Return the children of this enpoint, or an empty array if there are no children.
	 */
	Endpoint<?>[] getChildren();

}
