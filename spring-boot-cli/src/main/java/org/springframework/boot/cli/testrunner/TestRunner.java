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

import org.springframework.boot.cli.compiler.GroovyCompiler;

/**
 * Compile and run groovy based tests.
 * 
 * @author Phillip Webb
 */
public class TestRunner {

	private final File[] files;

	private final String[] args;

	private final GroovyCompiler compiler;

	/**
	 * Create a new {@link TestRunner} instance.
	 * @param configuration
	 * @param files
	 * @param args
	 */
	public TestRunner(TestRunnerConfiguration configuration, File[] files, String[] args) {
		this.files = files.clone();
		this.args = args.clone();
		this.compiler = new GroovyCompiler(configuration);
	}

	public void compileAndRunTests() throws Exception {
		// Object[] sources = this.compiler.sources(this.files);
		// if (sources.length == 0) {
		// throw new RuntimeException("No classes found in '" + this.files + "'");
		// }
		// Class<?> source = (Class<?>) sources[0];
		// Class<?> junitCore =
		// source.getClassLoader().loadClass(JUnitCore.class.getName());
		// Class<?> textListener = source.getClassLoader().loadClass(
		// TextListener.class.getName());
		// Object tl = textListener.getConstructor(PrintStream.class)
		// .newInstance(System.out);
		// Object ju = junitCore.newInstance();
		// Method method = junitCore.getDeclaredMethod("addListener", source
		// .getClassLoader().loadClass(RunListener.class.getName()));
		// method.invoke(ju, tl);
		// junitCore.getDeclaredMethod("run", Class[].class).invoke(ju,
		// new Object[] { new Class[] { source } });

		// JUnitCore jUnitCore = new JUnitCore();
		// jUnitCore.addListener(new TextListener(System.out));
		// jUnitCore.run((Class) sources[0]);
	}

}
