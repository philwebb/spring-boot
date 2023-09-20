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

package org.springframework.boot.loader.zip;

import java.nio.file.Path;

/**
 * @author pwebb
 */

import org.junit.jupiter.api.Test;

public class TempTests {

	@Test
	void testName() throws Exception {
		ZipContent.open(Path
			.of("/Users/pwebb/projects/spring-boot/code/3.2.x/spring-boot-tests/spring-boot-smoke-tests/spring-boot-smoke-test-ant/build/ant/libs/spring-boot-smoke-test-ant.jar"),
				"BOOT-INF/lib/spring-boot-autoconfigure-jar-3.2.0-SNAPSHOT.jar");
	}

}