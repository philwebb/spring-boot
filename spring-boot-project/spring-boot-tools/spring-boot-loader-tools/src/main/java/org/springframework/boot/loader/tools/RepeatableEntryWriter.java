/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link EntryWriter} that can be called multiple times.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class RepeatableEntryWriter implements EntryWriter {

	private final byte[] bytes;

	private RepeatableEntryWriter(EntryWriter writer) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writer.write(outputStream);
		this.bytes = outputStream.toByteArray(); // FIXME too much memory?
	}

	public long size() {
		return this.bytes.length;
	}

	@Override
	public void write(OutputStream outputStream) throws IOException {
		outputStream.write(this.bytes);
	}

	public static RepeatableEntryWriter get(EntryWriter entryWriter) throws IOException {
		if (entryWriter instanceof RepeatableEntryWriter) {
			return (RepeatableEntryWriter) entryWriter;
		}
		return new RepeatableEntryWriter(entryWriter);
	}

}
