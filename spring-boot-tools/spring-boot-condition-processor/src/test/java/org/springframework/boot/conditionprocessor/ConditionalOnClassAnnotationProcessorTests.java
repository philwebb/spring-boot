/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.conditionprocessor;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.junit.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnClassAnnotationProcessor}.
 *
 * @author Madhura Bhave
 */
public class ConditionalOnClassAnnotationProcessorTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TestCompiler compiler;

	@Before
	public void createCompiler() throws IOException {
		this.compiler = new TestCompiler(this.temporaryFolder);
	}

	@Test
	public void annotatedClass() throws Exception {
		Properties properties = compile(TestClassConfiguration.class);
		assertThat(properties).hasSize(1).containsEntry(
				"org.springframework.boot.conditionprocessor.TestClassConfiguration",
				"java.io.InputStream,java.io.OutputStream");
	}

	@Test
	public void annotatedMethod() throws Exception {
		Properties properties = compile(TestMethodConfiguration.class);
		assertThat(properties).isNull();
	}

	private Properties compile(Class<?>... types) throws IOException {
		TestConditionMetdataAnnotationProcessor processor = new TestConditionMetdataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
		return processor.getWrittenProperties();
	}

}
