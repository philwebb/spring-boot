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

package org.springframework.boot.loader;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.JarFileArchive;

/**
 * @author Phillip Webb
 */
public class TempTest {

	private static final AsciiBytes LIB = new AsciiBytes("lib/");

	@Test
	public void testName() throws Exception {
		long start = System.nanoTime();
		File root = new File(
				"/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
		// FileInputStream fileInputStream = new FileInputStream(root);
		// byte[] buffer = new byte[4096];
		// int bytesRead = -1;
		// while ((bytesRead = fileInputStream.read(buffer)) != -1) {
		// }

		JarFileArchive jarFileArchive = new JarFileArchive(root);
		jarFileArchive.getNestedArchives(new EntryFilter() {

			@Override
			public boolean matches(Entry entry) {
				return !entry.isDirectory() && entry.getName().startsWith(LIB);
			}
		});
		System.out.println(System.nanoTime());
		System.out.println((System.nanoTime() - start) / 100000000.0);
		// Raw read is 0.03616
	}

	@Test
	public void testName2() throws Exception {
		long start = System.nanoTime();
		File root = new File(
				"/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
		FileInputStream fileInputStream = new FileInputStream(root);
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = fileInputStream.read(buffer)) != -1) {
		}
		System.out.println((System.nanoTime() - start) / 100000000.0);
	}

	public static void main(String[] args) {
		try {
			new TempTest().testName();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	//
	// private static class FastZipInputStream extends ZipInputStream {
	//
	// private ZipEntry entry;
	//
	// public FastZipInputStream(InputStream in) {
	// super(in);
	// try {
	// makeFast();
	// }
	// catch (SecurityException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// catch (IllegalArgumentException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// catch (NoSuchFieldException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// catch (IllegalAccessException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// @Override
	// public ZipEntry getNextEntry() throws IOException {
	// this.entry = super.getNextEntry();
	// return this.entry;
	// }
	//
	// public void makeFast() throws SecurityException, NoSuchFieldException,
	// IllegalArgumentException, IllegalAccessException {
	// Field field = ZipInputStream.class.getDeclaredField("crc");
	// field.setAccessible(true);
	// field.set(this, new CRC32() {
	// @Override
	// public void update(int b) {
	// }
	//
	// @Override
	// public void update(byte[] b) {
	// }
	//
	// @Override
	// public void update(byte[] b, int off, int len) {
	// }
	//
	// @Override
	// public long getValue() {
	// return FastZipInputStream.this.entry.getCrc();
	// }
	// });
	// field = ZipInputStream.class.getDeclaredField("tmpbuf");
	// field.setAccessible(true);
	// field.set(this, new byte[10 * 1024]);
	// }
	//
	// }

}
