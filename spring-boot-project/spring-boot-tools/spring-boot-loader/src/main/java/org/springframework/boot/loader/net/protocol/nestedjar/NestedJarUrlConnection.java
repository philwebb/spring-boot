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

package org.springframework.boot.loader.net.protocol.nestedjar;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.SeekableByteChannel;

/**
 * A URL Connection providing the raw content of a nested JAR as a
 * {@link SeekableByteChannel}.
 * <p>
 * The syntax of a nested JAR URL is: <pre>
 * nestedjar:&lt;path&gt;!{entry}
 * </pre>
 * <p>
 * for example:
 * <p>
 * {@code nestedjar:/home/example/my.jar!BOOT-INF/lib/my-nested.jar}
 * <p>
 * The path must refer to a container jar file on the file system. The entry refers to the
 * uncompressed entry within the container jar that contains the nested jar. The entry
 * must not start with a {@code '/'}.
 *
 * @author Phillip Webb
 */
public class NestedJarUrlConnection extends URLConnection {

	protected NestedJarUrlConnection(URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public SeekableByteChannel getByteChannel() {
		return null;
	}

}
