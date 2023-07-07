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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * @author pwebb
 */
class JarUrlConnection extends java.net.JarURLConnection {

	protected JarUrlConnection(URL url) throws MalformedURLException {
		super(url);
	}

	@Override
	public JarFile getJarFile() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void connect() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
