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

package org.springframework.boot.developertools.filewatch;

import java.io.File;

import org.springframework.util.Assert;

/**
 * A single file that has changed.
 *
 * @author Phillip Webb
 */
public final class ChangedFile {

	private final File file;

	private final Type type;

	/**
	 * Create a new {@link ChangedFile} instance.
	 * @param file the file
	 * @param type the type of change
	 */
	ChangedFile(File file, Type type) {
		Assert.notNull(file, "File must not be null");
		Assert.notNull(type, "Type must not be null");
		this.file = file;
		this.type = type;
	}

	/**
	 * Return the file that was changed.
	 * @return the file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * Return the type of change.
	 * @return the type of change
	 */
	public Type getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		return this.file.hashCode() * 31 + this.type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof ChangedFile) {
			ChangedFile other = (ChangedFile) obj;
			return this.file.equals(other.file) && this.type.equals(other.type);
		}
		return super.equals(obj);
	}

	@Override
	public String toString() {
		return this.file + " (" + this.type + ")";
	}

	/**
	 * Change types.
	 */
	public static enum Type {

		/**
		 * A new file has been added.
		 */
		ADD,

		/**
		 * An existing file has been modified.
		 */
		MODIFY,

		/**
		 * An existing file has been deleted.
		 */
		DELETE

	}

}
