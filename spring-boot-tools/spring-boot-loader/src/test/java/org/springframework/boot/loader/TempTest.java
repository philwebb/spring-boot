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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.springframework.boot.loader.archive.Archive.Entry;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.util.FileCopyUtils;

/**
 * @author Phillip Webb
 */
public class TempTest {

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
				return false && !entry.isDirectory()
						&& entry.getName().startsWith("lib/");
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

	@Test
	public void testName3() throws Exception {
		long start = System.nanoTime();
		File root = new File(
				"/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
		JarFile jarFile = new JarFile(root);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
		}
		// FileInputStream fileInputStream = new FileInputStream(root);
		// byte[] buffer = new byte[4096];
		// int bytesRead = -1;
		// while ((bytesRead = fileInputStream.read(buffer)) != -1) {
		// }
		System.out.println((System.nanoTime() - start) / 100000000.0);
	}

	@Test
	public void testName4() throws Exception {
		File root = new File(
				"/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
		ByteArrayInputStream bis = new ByteArrayInputStream(
				FileCopyUtils.copyToByteArray(root));
		FileInputStream fis = new FileInputStream(root);
		long start = System.nanoTime();
		FastZipInputStream jarFile = new FastZipInputStream(bis);
		// jarFile.makeFast();

		ZipEntry nextEntry = jarFile.getNextEntry();
		while (nextEntry != null) {
			nextEntry = jarFile.getNextEntry();
		}
		System.out.println("test");
		// FileInputStream fileInputStream = new FileInputStream(root);
		// byte[] buffer = new byte[4096];
		// int bytesRead = -1;
		// while ((bytesRead = fileInputStream.read(buffer)) != -1) {
		// }
		System.out.println((System.nanoTime() - start) / 100000000.0);
	}

	public static void main(String[] args) {
		try {
			System.in.read();
			new TempTest().testName4();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class FastZipInputStream extends ZipInputStream {

		private ZipEntry entry;

		public FastZipInputStream(InputStream in) {
			super(in);
			try {
				makeFast();
			}
			catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public ZipEntry getNextEntry() throws IOException {
			this.entry = super.getNextEntry();
			return this.entry;
		}

		public void makeFast() throws SecurityException, NoSuchFieldException,
				IllegalArgumentException, IllegalAccessException {
			Field field = ZipInputStream.class.getDeclaredField("crc");
			field.setAccessible(true);
			field.set(this, new CRC32() {
				@Override
				public void update(int b) {
				}

				@Override
				public void update(byte[] b) {
				}

				@Override
				public void update(byte[] b, int off, int len) {
				}

				@Override
				public long getValue() {
					return FastZipInputStream.this.entry.getCrc();
				}
			});
			field = ZipInputStream.class.getDeclaredField("tmpbuf");
			field.setAccessible(true);
			field.set(this, new byte[10 * 1024]);
		}

	}

}
