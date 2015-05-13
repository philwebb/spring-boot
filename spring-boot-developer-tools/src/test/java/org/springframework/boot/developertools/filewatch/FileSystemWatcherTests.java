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

package org.springframework.boot.developertools.filewatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.developertools.filewatch.ChangedFile.Type;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link FileSystemWatcher}.
 *
 * @author Phillip Webb
 */
public class FileSystemWatcherTests {

	private FileSystemWatcher watcher;

	private List<Set<ChangedFiles>> changes = new ArrayList<Set<ChangedFiles>>();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Before
	public void setup() throws InterruptedException {
		// Set a modifiedAfterTime in the past to save the tests needing to wait
		this.watcher = createFileSystemWatcher(System.currentTimeMillis() - 2000);
	}

	@Test
	public void simpleFile() throws Exception {
		File folder = this.temp.newFolder();
		this.watcher.addSourceFolder(folder);
		File file = touch(new File(folder, "test.txt"));
		sleep();
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles(), contains(new ChangedFile(file, Type.MODIFY)));
	}

	@Test
	public void simpleNestedFile() throws Exception {
		File folder = this.temp.newFolder();
		this.watcher.addSourceFolder(folder);
		File file = touch(new File(new File(folder, "sub"), "text.txt"));
		sleep();
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles(), contains(new ChangedFile(file, Type.MODIFY)));
	}

	@Test
	public void waitsForIdleTime() throws Exception {
		File folder = this.temp.newFolder();
		this.watcher.addSourceFolder(folder);
		for (int i = 0; i < 20; i++) {
			touch(new File(folder, i + "test.txt"));
			Thread.sleep(100);
		}
		sleep();
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles().size(), equalTo(20));
	}

	@Test
	public void withExistingFiles() throws Exception {
		File folder = this.temp.newFolder();
		touch(new File(folder, "test.txt"));
		Thread.sleep(1100);
		this.watcher = createFileSystemWatcher(System.currentTimeMillis() + 100);
		this.watcher.addSourceFolder(folder);
		Thread.sleep(1100);
		File file = touch(new File(folder, "test2.txt"));
		sleep();
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles(), contains(new ChangedFile(file, Type.MODIFY)));

	}

	@Test
	public void multipleSources() throws Exception {
		File folder1 = this.temp.newFolder();
		File folder2 = this.temp.newFolder();
		this.watcher.addSourceFolder(folder1);
		this.watcher.addSourceFolder(folder2);
		File file1 = touch(new File(folder1, "test.txt"));
		File file2 = touch(new File(folder2, "test.txt"));
		sleep();
		Set<ChangedFiles> change = getSingleOnChange();
		assertThat(change.size(), equalTo(2));
		for (ChangedFiles changedFiles : change) {
			if (changedFiles.getSourceFolder().equals(folder1)) {
				assertEquals(
						new HashSet<ChangedFile>(Arrays.asList(new ChangedFile(file1,
								Type.MODIFY))), changedFiles.getFiles());
			}
			else {
				assertEquals(
						new HashSet<ChangedFile>(Arrays.asList(new ChangedFile(file2,
								Type.MODIFY))), changedFiles.getFiles());
			}
		}
	}

	@Test
	public void multipleListeners() throws Exception {
		File folder = this.temp.newFolder();
		final Set<ChangedFiles> listener2Changes = new LinkedHashSet<ChangedFiles>();
		this.watcher.addSourceFolder(folder);
		this.watcher.addListener(new FileChangeListener() {
			@Override
			public void onChange(Set<ChangedFiles> changeSet) {
				listener2Changes.addAll(changeSet);
			}
		});
		File file = touch(new File(folder, "test.txt"));
		sleep();
		ChangedFiles changedFiles = getSingleChangedFiles();
		assertThat(changedFiles.getFiles(), contains(new ChangedFile(file, Type.MODIFY)));
		assertEquals(this.changes.get(0), listener2Changes);
	}

	private ChangedFiles getSingleChangedFiles() {
		Set<ChangedFiles> singleChange = getSingleOnChange();
		assertThat(singleChange.size(), equalTo(1));
		return singleChange.iterator().next();
	}

	private Set<ChangedFiles> getSingleOnChange() {
		assertThat(this.changes.size(), equalTo(1));
		return this.changes.get(0);
	}

	private FileSystemWatcher createFileSystemWatcher(long modifiedAfterTime) {
		return new FileSystemWatcher(modifiedAfterTime, true, new FileChangeListener() {
			@Override
			public void onChange(Set<ChangedFiles> changeSet) {
				FileSystemWatcherTests.this.changes.add(changeSet);
			}
		});
	}

	private File touch(File file) throws FileNotFoundException, IOException {
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		fileOutputStream.close();
		return file;
	}

	private void sleep() throws InterruptedException {
		Thread.sleep(1500);
	}

}
