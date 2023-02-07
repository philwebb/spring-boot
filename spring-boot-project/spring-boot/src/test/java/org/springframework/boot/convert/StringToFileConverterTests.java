/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.convert;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringToFileConverter}.
 *
 * @author Phillip Webb
 */
class StringToFileConverterTests {

	@TempDir
	File temp;

	@ConversionServiceTest
	void convertWhenSimpleFileReturnsFile(ConversionService conversionService) {
		assertThat(convert(conversionService, this.temp.getAbsolutePath() + "/test"))
			.isEqualTo(new File(this.temp, "test").getAbsoluteFile());
	}

	@ConversionServiceTest
	void convertWhenFilePrefixedReturnsFile(ConversionService conversionService) {
		assertThat(convert(conversionService, "file:" + this.temp.getAbsolutePath() + "/test").getAbsoluteFile())
			.isEqualTo(new File(this.temp, "test").getAbsoluteFile());
	}

	private File convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, File.class);
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments
			.with((conversionService) -> conversionService.addConverter(new StringToFileConverter()));
	}

}
