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


/**
 * @author Phillip Webb
 */
public class TempTest {

	// @Test
	// public void testName() throws Exception {
	// long start = System.nanoTime();
	// File root = new File(
	// "/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
	// // FileInputStream fileInputStream = new FileInputStream(root);
	// // byte[] buffer = new byte[4096];
	// // int bytesRead = -1;
	// // while ((bytesRead = fileInputStream.read(buffer)) != -1) {
	// // }
	//
	// JarFileArchive jarFileArchive = new JarFileArchive(root);
	// jarFileArchive.getNestedArchives(new EntryFilter() {
	//
	// @Override
	// public boolean matches(Entry entry) {
	// return false && !entry.isDirectory()
	// && entry.getName().startsWith("lib/");
	// }
	// });
	// System.out.println(System.nanoTime());
	// System.out.println((System.nanoTime() - start) / 100000000.0);
	// // Raw read is 0.03616
	// }
	//
	// @Test
	// public void testName2() throws Exception {
	// long start = System.nanoTime();
	// File root = new File(
	// "/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
	// FileInputStream fileInputStream = new FileInputStream(root);
	// byte[] buffer = new byte[4096];
	// int bytesRead = -1;
	// while ((bytesRead = fileInputStream.read(buffer)) != -1) {
	// }
	// System.out.println((System.nanoTime() - start) / 100000000.0);
	// }
	//
	// @Test
	// public void testName3() throws Exception {
	// long start = System.nanoTime();
	// File root = new File(
	// "/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
	// JarFile jarFile = new JarFile(root);
	// Enumeration<JarEntry> entries = jarFile.entries();
	// while (entries.hasMoreElements()) {
	// JarEntry jarEntry = entries.nextElement();
	// }
	// // FileInputStream fileInputStream = new FileInputStream(root);
	// // byte[] buffer = new byte[4096];
	// // int bytesRead = -1;
	// // while ((bytesRead = fileInputStream.read(buffer)) != -1) {
	// // }
	// System.out.println((System.nanoTime() - start) / 100000000.0);
	// }
	//
	// @Test
	// public void testName4() throws Exception {
	// File root = new File(
	// "/Users/pwebb/projects/spring-boot/code/spring-boot-cli/target/spring-boot-cli-0.5.0.BUILD-SNAPSHOT-full.jar");
	// ByteArrayInputStream bis = new ByteArrayInputStream(
	// FileCopyUtils.copyToByteArray(root));
	// FileInputStream fis = new FileInputStream(root);
	// long start = System.nanoTime();
	// FastZipInputStream jarFile = new FastZipInputStream(bis);
	// // jarFile.makeFast();
	//
	// ZipEntry nextEntry = jarFile.getNextEntry();
	// while (nextEntry != null) {
	// nextEntry = jarFile.getNextEntry();
	// }
	// System.out.println("test");
	// // FileInputStream fileInputStream = new FileInputStream(root);
	// // byte[] buffer = new byte[4096];
	// // int bytesRead = -1;
	// // while ((bytesRead = fileInputStream.read(buffer)) != -1) {
	// // }
	// System.out.println((System.nanoTime() - start) / 100000000.0);
	// }
	//
	// @Test
	// public void stringPerf() throws Exception {
	// byte[] bytes = "META-INF/MANIFEST.MF/test/test/test//test/test/test/.test"
	// .getBytes();
	// long start = System.nanoTime();
	// for (int i = 0; i < 7000; i++) {
	// char[] c = new char[bytes.length];
	// for (int i1 = 0; i1 < c.length; i1++) {
	// c[i1] = (char) bytes[i1];
	// }
	// new String(c); // fastToString3(bytes);
	// }
	// System.out.println((System.nanoTime() - start) / 100000000.0);
	// System.out.println(fastToString2(bytes));
	// }
	//
	// // 0.39
	// private String fastToString0(byte[] bytes) throws UnsupportedEncodingException {
	// return new String(bytes, "UTF-8");
	// }
	//
	// // 0.22
	// private String fastToString1(byte[] bytes) {
	// StringBuilder builder = new StringBuilder(bytes.length);
	// for (byte b : bytes) {
	// builder.append((char) b);
	// }
	// return builder.toString();
	// }
	//
	// // 0.16
	// private String fastToString2(byte[] bytes) {
	// char[] c = new char[bytes.length];
	// for (int i = 0; i < c.length; i++) {
	// c[i] = (char) bytes[i];
	// }
	// return new String(c);
	// }
	//
	// static Constructor<String> stringcon;
	// static {
	// try {
	// Constructor<String> declaredConstructor = String.class
	// .getDeclaredConstructor(int.class, int.class, char[].class);
	// declaredConstructor.setAccessible(true);
	// stringcon = declaredConstructor;
	//
	// }
	// catch (SecurityException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// catch (NoSuchMethodException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	// // 0.16
	// private String fastToString3(byte[] bytes) throws IllegalArgumentException,
	// InstantiationException, IllegalAccessException, InvocationTargetException {
	// char[] c = new char[bytes.length];
	// for (int i = 0; i < c.length; i++) {
	// c[i] = (char) bytes[i];
	// }
	// return stringcon.newInstance(0, c.length, c);
	// }
	//
	// public static void main(String[] args) {
	//
	// try {
	// // System.in.read();
	// new TempTest().stringPerf();
	// }
	// catch (Exception ex) {
	// ex.printStackTrace();
	// }
	// }
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
