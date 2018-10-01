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

package org.springframework.boot.devtools.restart.classloader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MyExpectedException;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ClassLoaderFile}.
 *
 * @author Phillip Webb
 */
public class ClassLoaderFileTests {

	public static final byte[] BYTES = "ABC".getBytes();

	@Rule
	public MyExpectedException thrown = MyExpectedException.none();

	@Test
	public void kindMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class, () -> new ClassLoaderFile(null, null),
				"Kind must not be null");
	}

	@Test
	public void addedContentsMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class, () -> new ClassLoaderFile(Kind.ADDED, null),
				"Contents must not be null");
	}

	@Test
	public void modifiedContentsMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class, () -> new ClassLoaderFile(Kind.MODIFIED, null),
				"Contents must not be null");
	}

	@Test
	public void deletedContentsMustBeNull() {
		this.thrown.expect(IllegalArgumentException.class, () -> new ClassLoaderFile(Kind.DELETED, new byte[10]),
				"Contents must be null");
	}

	@Test
	public void added() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.ADDED, BYTES);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.ADDED);
		assertThat(file.getContents()).isEqualTo(BYTES);
	}

	@Test
	public void modified() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.MODIFIED, BYTES);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.MODIFIED);
		assertThat(file.getContents()).isEqualTo(BYTES);
	}

	@Test
	public void deleted() {
		ClassLoaderFile file = new ClassLoaderFile(Kind.DELETED, null);
		assertThat(file.getKind()).isEqualTo(ClassLoaderFile.Kind.DELETED);
		assertThat(file.getContents()).isNull();
	}

}
