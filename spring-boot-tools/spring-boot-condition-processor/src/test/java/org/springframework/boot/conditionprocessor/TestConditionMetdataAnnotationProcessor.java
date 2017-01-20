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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Version of {@link ConditionalOnClassAnnotationProcessor} used for testing.
 *
 * @author Madhura Bhave
 */
@SupportedAnnotationTypes("org.springframework.boot.conditionprocessor.TestConditionalOnClass")
public class TestConditionMetdataAnnotationProcessor
		extends ConditionalOnClassAnnotationProcessor {

	private final File outputLocation;

	public TestConditionMetdataAnnotationProcessor(File outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	protected String conditionalOnClassAnnotation() {
		return "org.springframework.boot.conditionprocessor.TestConditionalOnClass";
	}

	public Properties getWrittenProperties() throws IOException {
		File file = new File(this.outputLocation, PROPERTIES_PATH);
		if (!file.exists()) {
			return null;
		}
		FileInputStream inputStream = new FileInputStream(file);
		try {
			Properties properties = new Properties();
			properties.load(inputStream);
			return properties;
		}
		finally {
			inputStream.close();
		}
	}

}
