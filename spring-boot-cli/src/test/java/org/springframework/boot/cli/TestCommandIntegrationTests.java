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

package org.springframework.boot.cli;


/**
 * Integration tests to exercise the CLI's test command.
 * 
 * @author Greg Turnquist
 */
public class TestCommandIntegrationTests {

	// @BeforeClass
	// public static void cleanGrapes() throws Exception {
	// GrapesCleaner.cleanIfNecessary();
	// // System.setProperty("ivy.message.logger.level", "3");
	// }
	//
	// @Before
	// public void setup() throws Exception {
	// System.setProperty("disableSpringSnapshotRepos", "true");
	// new CleanCommand().run("org.springframework");
	// }
	//
	// @After
	// public void teardown() {
	// System.clearProperty("disableSpringSnapshotRepos");
	// }
	//
	// @Test
	// public void noTests() throws Throwable {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/book.groovy");
	// TestResults results = command.getResults();
	// assertEquals(0, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
	// @Test
	// public void empty() throws Exception {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/empty.groovy");
	// TestResults results = command.getResults();
	// assertEquals(0, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
	// @Test(expected = RuntimeException.class)
	// public void noFile() throws Exception {
	// try {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/nothing.groovy");
	// }
	// catch (RuntimeException e) {
	// assertEquals("Can't find samples/nothing.groovy", e.getMessage());
	// throw e;
	// }
	// }
	//
	// @Test
	// public void appAndTestsInOneFile() throws Exception {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/book_and_tests.groovy");
	// TestResults results = command.getResults();
	// assertEquals(1, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
	// @Test
	// public void appInOneFileTestsInAnotherFile() throws Exception {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/book.groovy", "samples/test.groovy");
	// TestResults results = command.getResults();
	// assertEquals(1, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
	// @Test
	// public void spockTester() throws Exception {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/spock.groovy");
	// TestResults results = command.getResults();
	// assertEquals(1, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
	// @Test
	// public void spockAndJunitTester() throws Exception {
	// XTestCommand command = new XTestCommand();
	// command.run("samples/spock.groovy", "samples/book_and_tests.groovy");
	// TestResults results = command.getResults();
	// assertEquals(2, results.getRunCount());
	// assertEquals(0, results.getFailureCount());
	// assertTrue(results.wasSuccessful());
	// }
	//
}
