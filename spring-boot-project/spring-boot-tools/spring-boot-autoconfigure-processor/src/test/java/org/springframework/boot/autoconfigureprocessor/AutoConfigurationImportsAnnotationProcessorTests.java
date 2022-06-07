/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigureprocessor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.compile.TestCompiler;
import org.springframework.aot.test.generate.file.SourceFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationImportsAnnotationProcessor}.
 *
 * @author Scott Frederick
 * @author Stephane Nicoll
 */
class AutoConfigurationImportsAnnotationProcessorTests {

	@Test
	void annotatedClasses() throws Exception {
		compile(Arrays.asList(TestAutoConfigurationOnlyConfiguration.class, TestAutoConfigurationConfiguration.class),
				(classes) -> {
					assertThat(classes).hasSize(2);
					assertThat(classes).containsExactly(
							"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationConfiguration",
							"org.springframework.boot.autoconfigureprocessor.TestAutoConfigurationOnlyConfiguration");

				});
	}

	@Test
	void notAnnotatedClasses() throws Exception {
		compile(Collections.singletonList(TestAutoConfigurationImportsAnnotationProcessor.class),
				(classes) -> assertThat(classes).isNull());
	}

	private void compile(Collection<Class<?>> types, Consumer<List<String>> consumer) {
		TestAutoConfigurationImportsAnnotationProcessor processor = new TestAutoConfigurationImportsAnnotationProcessor();
		List<SourceFile> sources = types.stream().map(SourceFile::forTestClass).toList();
		TestCompiler compiler = TestCompiler.forSystem().withProcessors(processor).withSources(sources);
		compiler.compile((compiled) -> {
			InputStream importsFile = compiled.getClassLoader().getResourceAsStream(processor.getImportsFilePath());
			consumer.accept(getWrittenImports(importsFile));
		});
	}

	private List<String> getWrittenImports(InputStream inputStream) {
		if (inputStream == null) {
			return null;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		return reader.lines().collect(Collectors.toList());
	}

}
