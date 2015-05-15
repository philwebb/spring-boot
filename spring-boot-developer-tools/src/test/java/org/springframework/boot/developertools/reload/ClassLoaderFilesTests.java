/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.reload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.developertools.reload.ClassLoaderFile.Kind;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClassLoaderFiles}.
 *
 * @author Phillip Webb
 */
public class ClassLoaderFilesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ClassLoaderFiles files = new ClassLoaderFiles();

	@Test
	public void addFileNameMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		this.files.addFile(null, mock(ClassLoaderFile.class));
	}

	@Test
	public void addFileFileMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("File must not be null");
		this.files.addFile("test", null);
	}

	@Test
	public void getFileWithNullName() throws Exception {
		assertThat(this.files.getFile(null), nullValue());
	}

	@Test
	public void addAndGet() throws Exception {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, new byte[10]);
		this.files.addFile("myfile", file);
		assertThat(this.files.getFile("myfile"), equalTo(file));
	}

	@Test
	public void getMissing() throws Exception {

	}

	@Test
	public void addTwice() throws Exception {

	}

}
