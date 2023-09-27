/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;

import org.springframework.boot.loader.jar.JarFile;

class TempClassicTests {

	// @formatter:off

	@Test
	void time() throws Exception {
		long start = System.nanoTime();
		String filename = "/Users/pwebb/projects/spring-boot/code/3.2.x/spring-boot-tests/spring-boot-integration-tests/spring-boot-server-tests/build/spring-boot-server-tests-app/build/libs/spring-boot-server-tests-app-tomcat.jar";
		try (JarFile jarFile = new JarFile(new File(filename))) {
			list(jarFile);
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-server-tests-app-resources.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-web-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-starter-3.2.0-SNAPSHOT.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-starter-tomcat-3.2.0-SNAPSHOT.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-autoconfigure-3.2.0-SNAPSHOT.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-3.2.0-SNAPSHOT.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-context-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-aop-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-beans-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-expression-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-core-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/micrometer-observation-1.12.0-M3.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-starter-logging-3.2.0-SNAPSHOT.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/jakarta.annotation-api-2.1.1.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/snakeyaml-2.2.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/tomcat-embed-websocket-10.1.13.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/tomcat-embed-core-10.1.13.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/tomcat-embed-el-10.1.13.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-jcl-6.1.0-M5.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/micrometer-commons-1.12.0-M3.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/logback-classic-1.4.11.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/log4j-to-slf4j-2.20.0.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/jul-to-slf4j-2.0.9.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/logback-core-1.4.11.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/slf4j-api-2.0.9.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/log4j-api-2.20.0.jar")));
			list(jarFile.getNestedJarFile(jarFile.getEntry("BOOT-INF/lib/spring-boot-jarmode-layertools-3.2.0-SNAPSHOT.jar")));
			System.out.println(TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - start));
		}
	}

	private void list(JarFile jarFile) {
		Enumeration<JarEntry> entries = jarFile.entries();
		while(entries.hasMoreElements()) {
			entries.nextElement();
		}
	}

	// @formatter:on

}
