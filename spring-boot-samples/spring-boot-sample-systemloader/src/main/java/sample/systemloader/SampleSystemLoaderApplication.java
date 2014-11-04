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

package sample.systemloader;

import sample.systemloader.security.CustomSecurityManager2;

/**
 * Sample to show system classloading with fat jars.
 *
 * @author Phillip Webb
 */
public class SampleSystemLoaderApplication {

	public static void main(String[] args) {
		SecurityManager securityManager = System.getSecurityManager();
		if (securityManager != null) {
			System.out.println(securityManager.getClass());
		}
		if (securityManager == null
				|| securityManager.getClass() != CustomSecurityManager2.class) {
			System.out.println("Run with "
					+ "-Djava.security.manager=sample.systemloader."
					+ "security.CustomSecurityManager");
			System.exit(1);
		}
	}

}
