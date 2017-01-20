package org.springframework.boot.conditionprocessor;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.junit.runner.classpath.TestCompiler;

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
	public void conditionalOnClassMetadata() throws Exception {
		compile(TestConditional.class);
	}

	private void compile(Class<?>... types) throws IOException {
		TestConditionMetdataAnnotationProcessor processor = new TestConditionMetdataAnnotationProcessor(
				this.compiler.getOutputLocation());
		this.compiler.getTask(types).call(processor);
	}


}