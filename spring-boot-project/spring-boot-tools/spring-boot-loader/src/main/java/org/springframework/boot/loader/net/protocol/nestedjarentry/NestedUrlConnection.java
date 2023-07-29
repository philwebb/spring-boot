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

package org.springframework.boot.loader.net.protocol.nestedjarentry;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * A URL Connection...
 * <p>
 * The syntax of a nested JAR URL is: <pre>
 * nestedjar:&lt;path&gt;!{entry}
 * </pre>
 * <p>
 * for example:
 * <p>
 * {@code nested:/home/example/my.jar!BOOT-INF/lib/my-nested.jar}
 * <p>
 * The path must refer to a container jar file on the file system. The entry refers to the
 * uncompressed entry within the container jar that contains the nested jar. The entry
 * must not start with a {@code '/'}.
 *
 * @author Phillip Webb
 */
public class NestedUrlConnection extends URLConnection {

	/*
	 * jar:file:foo.jar!/BOOT-INF/lib/spring-core.jar!/org/spring/Utils.class
	 *
	 * jar:<innerurl>!/org/spring/Utils.class
	 *
	 * jar:nested:foo.jar!BOOT-INF/lib/spring-core.jar!/org/spring/Utils.class
	 *
	 *
	 * jar:file:foo.jar!/BOOT-INF/lib/spring-core.jar!/org/spring/Utils.class
	 * jar:http://foo.jar!/BOOT-INF/lib/spring-core.jar!/org/spring/Utils.class
	 */

	protected NestedUrlConnection(URL url) {
		super(url);
	}

	@Override
	public void connect() throws IOException {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	// don't do much other than expose paths

}
