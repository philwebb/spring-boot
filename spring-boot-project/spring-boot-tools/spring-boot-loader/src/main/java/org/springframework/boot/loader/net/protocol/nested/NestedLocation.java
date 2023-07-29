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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.net.URL;

import org.springframework.boot.loader.net.protocol.UrlDecoder;

/**
 * A nested location from a {@code nested:} {@link URL} path.
 *
 * @param file the zip file that contains the nested entry
 * @param nestedEntryName the nested entry name
 * @author Phillip Webb
 * @since 3.2.0
 */
public record NestedLocation(File file, String nestedEntryName) {

	public static NestedLocation fromUrl(URL url) {
		if (url == null || !"nested".equalsIgnoreCase(url.getProtocol())) {
			throw new IllegalArgumentException("'url' must not be null and must use 'nested' protocol");
		}
		return parse(UrlDecoder.decode(url.getPath()));
	}

	private static NestedLocation parse(String location) {
		if (location == null || location.isEmpty()) {
			throw new IllegalArgumentException("'location' must not be null and or empty");
		}
		int index = location.lastIndexOf('!');
		if (index == -1) {
			throw new IllegalArgumentException("'location' must contain '!'");
		}
		String file = location.substring(0, index);
		String nestedEntryName = location.substring(index, location.length());
		return new NestedLocation(new File(file), nestedEntryName);
	}

}
