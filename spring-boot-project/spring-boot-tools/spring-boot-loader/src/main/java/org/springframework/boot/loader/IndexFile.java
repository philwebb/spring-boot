/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author pwebb
 */
public class IndexFile {

	public static final IndexFile NONE = null;

	/**
	 * @param url
	 * @param location
	 * @return
	 */
	public static IndexFile loadFromFile(URL parent, String location) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	private File asFile(URL url) {
		try {
			return new File(url.toURI());
		}
		catch (URISyntaxException ex) {
			return new File(url.getPath());
		}
	}

	// private final List<String> indexed = new ArrayList<>();
	// private static final int BUFFER_SIZE = 4096;
	// private void initializeIndex() throws IOException {
	// File index = getClassPathIndexFile(this.archive.getUrl().getFile());
	// if (index != null) {
	// FileInputStream fileInputStream = new FileInputStream(index);
	// String[] libs = copyToString(fileInputStream,
	// StandardCharsets.UTF_8).split("\r\n");
	// this.indexed.addAll(Arrays.asList(libs));
	// }
	// }
	//
	// private static String copyToString(InputStream in, Charset charset) throws
	// IOException {
	// if (in == null) {
	// return "";
	// }
	// StringBuilder out = new StringBuilder();
	// InputStreamReader reader = new InputStreamReader(in, charset);
	// char[] buffer = new char[BUFFER_SIZE];
	// int bytesRead = -1;
	// while ((bytesRead = reader.read(buffer)) != -1) {
	// out.append(buffer, 0, bytesRead);
	// }
	// return out.toString();
	// }
	//
	//
	//
	// private static final class IndexedFile {
	//
	// private final String root;
	//
	// private final String name;
	//
	// private IndexedFile(String root, String name) {
	// this.root = root;
	// this.name = name;
	// }
	//
	// String getName() {
	// return this.name;
	// }
	//
	// String getRoot() {
	// return this.root;
	// }
	//
	// }

}
