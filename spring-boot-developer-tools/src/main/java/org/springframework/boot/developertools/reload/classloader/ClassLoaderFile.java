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

package org.springframework.boot.developertools.reload.classloader;

import org.springframework.util.Assert;

/**
 * A single file that may be served from a {@link ClassLoader}. Can be used to represent
 * files that have been added, modified or deleted since the original JAR was created.
 *
 * @author Phillip Webb
 * @see ClassLoaderFileRepository
 * @since 1.3.0
 */
public class ClassLoaderFile {

	private final Kind kind;

	private final byte[] contents;

	public ClassLoaderFile(Kind kind, byte[] contents) {
		Assert.notNull(kind, "Kind must not be null");
		Assert.isTrue(kind == Kind.DELETED ? contents == null : contents != null,
				"Contents must " + (kind == Kind.DELETED ? "" : "not ") + "be null");
		this.kind = kind;
		this.contents = contents;
	}

	/**
	 * Return the file {@link Kind} (added, modified, deleted).
	 * @return the kind
	 */
	public Kind getKind() {
		return this.kind;
	}

	/**
	 * Return the contents of the file as a byte array or {@code null} if
	 * {@link #getKind()} is {@link Kind#DELETED}.
	 * @return the contents or {@code null}
	 */
	public byte[] getContents() {
		return this.contents;
	}

	/**
	 * The kinds of class load files.
	 */
	public static enum Kind {

		/**
		 * The file has been added since the original JAR was created.
		 */
		ADDED,

		/**
		 * The file has been modified since the original JAR was created.
		 */
		MODIFIED,

		/**
		 * The file has been deleted since the original JAR was created.
		 */
		DELETED

	}

}
