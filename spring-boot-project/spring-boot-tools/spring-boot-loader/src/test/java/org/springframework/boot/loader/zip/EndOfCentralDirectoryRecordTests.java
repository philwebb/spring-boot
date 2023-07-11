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

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.xzip.TestJarCreator;

/**
 * @author Phillip Webb
 */
class EndOfCentralDirectoryRecordTests {

	private File jarFile;

	private FileChannelDataBlock dataBlock;

	@BeforeEach
	void setup(@TempDir File tempDir) throws Exception {
		this.jarFile = new File(tempDir, "test.jar");
		TestJarCreator.createTestJar(this.jarFile);
		this.dataBlock = new FileChannelDataBlock(FileChannel.open(this.jarFile.toPath()));
	}

	@AfterEach
	void cleanup() throws IOException {
		this.dataBlock.close();
	}

	@Test
	void test() throws IOException {
		EndOfCentralDirectoryRecord.find(this.dataBlock);
	}

}
