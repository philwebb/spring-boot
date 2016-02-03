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

package org.springframework.boot.loader.jar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AsciiBytes}.
 *
 * @author Phillip Webb
 */
public class AsciiBytesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createFromBytes() throws Exception {
		AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66 });
		assertThat(bytes.toString()).isEqualTo("AB");
	}

	@Test
	public void createFromBytesWithOffset() throws Exception {
		AsciiBytes bytes = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		assertThat(bytes.toString()).isEqualTo("BC");
	}

	@Test
	public void createFromString() throws Exception {
		AsciiBytes bytes = new AsciiBytes("AB");
		assertThat(bytes.toString()).isEqualTo("AB");
	}

	@Test
	public void length() throws Exception {
		AsciiBytes b1 = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes b2 = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		assertThat(b1.length()).isEqualTo(2);
		assertThat(b2.length()).isEqualTo(2);
	}

	@Test
	public void startWith() throws Exception {
		AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
		AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abc.startsWith(abc))isTrue();
		assertThat(abc.startsWith(ab))isTrue();
		assertThat(abc.startsWith(bc))isFalse();
		assertThat(abc.startsWith(abcd))isFalse();
	}

	@Test
	public void endsWith() throws Exception {
		AsciiBytes abc = new AsciiBytes(new byte[] { 65, 66, 67 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67 }, 1, 2);
		AsciiBytes ab = new AsciiBytes(new byte[] { 65, 66 });
		AsciiBytes aabc = new AsciiBytes(new byte[] { 65, 65, 66, 67 });
		assertThat(abc.endsWith(abc))isTrue();
		assertThat(abc.endsWith(bc))isTrue();
		assertThat(abc.endsWith(ab))isFalse();
		assertThat(abc.endsWith(aabc))isFalse();
	}

	@Test
	public void substringFromBeingIndex() throws Exception {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abcd.substring(0).toString()).isEqualTo("ABCD");
		assertThat(abcd.substring(1).toString()).isEqualTo("BCD");
		assertThat(abcd.substring(2).toString()).isEqualTo("CD");
		assertThat(abcd.substring(3).toString()).isEqualTo("D");
		assertThat(abcd.substring(4).toString()).isEqualTo("");
		this.thrown.expect(IndexOutOfBoundsException.class);
		abcd.substring(5);
	}

	@Test
	public void substring() throws Exception {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		assertThat(abcd.substring(0, 4).toString()).isEqualTo("ABCD");
		assertThat(abcd.substring(1, 3).toString()).isEqualTo("BC");
		assertThat(abcd.substring(3, 4).toString()).isEqualTo("D");
		assertThat(abcd.substring(3, 3).toString()).isEqualTo("");
		this.thrown.expect(IndexOutOfBoundsException.class);
		abcd.substring(3, 5);
	}

	@Test
	public void appendString() throws Exception {
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		AsciiBytes appended = bc.append("D");
		assertThat(bc.toString()).isEqualTo("BC");
		assertThat(appended.toString()).isEqualTo("BCD");
	}

	@Test
	public void appendBytes() throws Exception {
		AsciiBytes bc = new AsciiBytes(new byte[] { 65, 66, 67, 68 }, 1, 2);
		AsciiBytes appended = bc.append(new byte[] { 68 });
		assertThat(bc.toString()).isEqualTo("BC");
		assertThat(appended.toString()).isEqualTo("BCD");
	}

	@Test
	public void hashCodeAndEquals() throws Exception {
		AsciiBytes abcd = new AsciiBytes(new byte[] { 65, 66, 67, 68 });
		AsciiBytes bc = new AsciiBytes(new byte[] { 66, 67 });
		AsciiBytes bc_substring = new AsciiBytes(new byte[] { 65, 66, 67, 68 })
				.substring(1, 3);
		AsciiBytes bc_string = new AsciiBytes("BC");
		assertThat(bc.hashCode()).isEqualTo(bc.hashCode());
		assertThat(bc.hashCode()).isEqualTo(bc_substring.hashCode());
		assertThat(bc.hashCode()).isEqualTo(bc_string.hashCode());
		assertThat(bc).isEqualTo(bc);
		assertThat(bc).isEqualTo(bc_substring);
		assertThat(bc).isEqualTo(bc_string);
		assertThat(bc.hashCode()).isNotEqualTo(abcd.hashCode());
		assertThat(bc).isNotEqualTo(abcd);
	}

	@Test
	public void hashCodeSameAsString() throws Exception {
		String s = "abcABC123xyz!";
		AsciiBytes a = new AsciiBytes(s);
		assertThat(s.hashCode()).isEqualTo(a.hashCode());
	}

	@Test
	public void hashCodeSameAsStringWithSpecial() throws Exception {
		String s = "special/\u00EB.dat";
		AsciiBytes a = new AsciiBytes(s);
		assertThat(s.hashCode()).isEqualTo(a.hashCode());
	}

}
