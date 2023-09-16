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

package org.springframework.boot.loader.net.protocol.jar;

import java.net.URL;

/**
 * Key generated from a jar file {@link URL} that can be used as a cache key.
 *
 * @author Phillip Webb
 */
class JarFileUrlKey {

	private final String value;

	private JarFileUrlKey(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((JarFileUrlKey) obj).value);
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Get the {@link JarFileUrlKey} for the given URL.
	 * @param url the source URL
	 * @return a {@link JarFileUrlKey} instance
	 */
	static JarFileUrlKey get(URL url) {
		StringBuilder value = new StringBuilder();
		String protocol = url.getProtocol();
		String host = url.getHost();
		int port = (url.getPort() != -1) ? url.getPort() : url.getDefaultPort();
		String file = url.getFile();
		value.append((protocol != null) ? protocol.toLowerCase() + "://" : "");
		if (host != null) {
			value.append(host.toLowerCase());
			value.append((port != -1) ? ":" + port : "");
		}
		value.append((file != null) ? file : "");
		if ("runtime".equals(url.getRef())) {
			value.append("#runtime");
		}
		return new JarFileUrlKey(value.toString());
	}

}
