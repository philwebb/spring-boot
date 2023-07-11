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

package org.springframework.boot.loader.zip;

/**
 * @author pwebb
 */
public class Dunno {

	// @formatter:off

	/*





	*/

// @formatter:on

	void dunno() {
		EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = null;
		Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = null;
		Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = null;

		long sizeOfCentralDirectory = endOfCentralDirectoryRecord.sizeOfCentralDirectory();
		long specifiedOffset = (zip64EndOfCentralDirectoryRecord != null)
				? zip64EndOfCentralDirectoryRecord.offsetToStartOfZip64CentralDirectory()
				: endOfCentralDirectoryRecord.pos();


		long sizeOfEOCD64 = zip64EndOfCentralDirectoryLocator.offsetToZip64EndOfCentralDirectoryRecord() - zip64EndOfCentralDirectoryLocator.pos();


		long zip64EndSize = (zip64EndOfCentralDirectoryRecord != null) ? sizeOfEOCD64 : 0L;
		int zip64LocSize = (zip64EndOfCentralDirectoryRecord != null) ? Zip64EndOfCentralDirectoryLocator.RECORD_SIZE: 0;
		long totalSizeOfTheFileIncludingAnyPreamble;
		long sizeOfEocd;
		long actualOffset = totalSizeOfTheFileIncludingAnyPreamble - sizeOfEocd - sizeOfCentralDirectory - zip64EndSize - zip64LocSize;
		 actualOffset - specifiedOffset;

	}

}
