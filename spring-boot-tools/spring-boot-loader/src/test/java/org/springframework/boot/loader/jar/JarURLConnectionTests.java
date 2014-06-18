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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.net.URL;

import org.junit.Test;

/**
 * Tests fpr {@link JarURLConnection}.
 * 
 * @author Phillip Webb
 */
public class JarURLConnectionTests {

	@Test
	public void time() throws Exception {
		long t = System.nanoTime();
		JarFile jarFile = new JarFile(new File("/Users/pwebb/projects/spring-boot/code/"
				+ "spring-boot-samples/spring-boot-sample-tomcat/target/"
				+ "spring-boot-sample-tomcat-1.1.2.BUILD-SNAPSHOT.jar"));
		for (int i = 0; i < 90000; i++) {
			JarURLConnection connection = new JarURLConnection(
					new URL("jar:file:/Users/pwebb/projects/spring-boot/code/"
							+ "spring-boot-samples/spring-boot-sample-tomcat/target/"
							+ "spring-boot-sample-tomcat-1.1.2.BUILD-SNAPSHOT.jar!/" + i),
					jarFile);
			try {
				connection.connect();
				// connection.getInputStream();
			}
			catch (Exception ex) {
			}
			if (i == 0) {
				t = System.nanoTime();
			}
		}
		System.out.println((System.nanoTime() - t) / 1000000000.0);
	}

}
