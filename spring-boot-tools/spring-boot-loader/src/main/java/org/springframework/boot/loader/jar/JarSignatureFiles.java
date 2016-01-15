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

package org.springframework.boot.loader.jar;

import org.springframework.boot.loader.data.RandomAccessData;

/**
 * @author Phillip Webb
 */
class JarSignatureFiles implements CentralDirectoryVistor {

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

	private boolean signed;

	@Override
	public void visitStart(CentralDirectoryEndRecord endRecord,
			RandomAccessData centralDirectoryData) {
	}

	@Override
	public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
		AsciiBytes name = fileHeader.getName();
		if (name.startsWith(META_INF) && name.endsWith(SIGNATURE_FILE_EXTENSION)) {
			this.signed = true;
		}
	}

	@Override
	public void visitEnd() {
	}

	public boolean isSigned() {
		return this.signed;
	}
}
