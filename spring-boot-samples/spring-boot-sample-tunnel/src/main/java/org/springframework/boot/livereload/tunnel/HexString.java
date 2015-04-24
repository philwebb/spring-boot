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

package org.springframework.boot.livereload.tunnel;

import java.nio.ByteBuffer;

/**
 * @author pwebb
 */
public class HexString {

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String toString(ByteBuffer payload) {
		return toString(payload.array(), payload.position(), payload.remaining());
	}

	private static String toString(byte[] bytes, int offset, int length) {
		char[] hexChars = new char[length * 2];
		for (int j = offset; j < length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
