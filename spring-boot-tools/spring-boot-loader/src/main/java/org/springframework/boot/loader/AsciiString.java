/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader;

/**
 * @author Phillip Webb
 */
public class AsciiString {

	private byte[] bytes;

	public AsciiString(byte[] bytes) {
		this.bytes = bytes;
	}

	public AsciiString(String string) {
		// TODO Auto-generated constructor stub
	}

	public int length() {
		return this.bytes.length;
	}

	/**
	 * @param metaInf
	 * @return
	 */
	public boolean startsWith(AsciiString metaInf) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param length
	 * @return
	 */
	public AsciiString substring(long length) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param i
	 * @param j
	 * @return
	 */
	public String substring(int i, int j) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param dotJar
	 * @return
	 */
	public boolean endsWith(AsciiString dotJar) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
