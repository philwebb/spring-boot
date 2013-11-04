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

package org.springframework.boot.cli.testrunner;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;

/**
 * @author Phillip Webb
 */
@Ignore
public class TestRunnerTests {

	@Test
	public void testName() throws Exception {

		TestRunnerConfiguration configuration = new TestRunnerConfiguration() {

			@Override
			public GroovyCompilerScope getScope() {
				throw new UnsupportedOperationException("Auto-generated method stub");
			}

			@Override
			public boolean isGuessImports() {
				return true;
			}

			@Override
			public boolean isGuessDependencies() {
				return true;
			}

			@Override
			public String[] getClasspath() {
				return NO_CLASSPATH;
			}
		};

		File[] filesArray = new File[] { new File(
				"src/test/resources/failing-test.groovy") };
		String[] argsArray = new String[] {};
		TestRunner testRunner = new TestRunner(configuration, filesArray, argsArray);
		testRunner.compileAndRunTests();
	}

}
