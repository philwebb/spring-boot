/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A configuration property name composed of elements separated by dots. Names may contain
 * the characters "{@code a-z}" "{@code 0-9}") and "{@code -}", they must be lower-case
 * and must start with a letter. The "{@code -}" is used purely for formatting, i.e.
 * "{@code foo-bar}" and "{@code foobar}" are considered equivalent.
 * <p>
 * The "{@code [}" and "{@code ]}" characters may be used to indicate an associative
 * index(i.e. a {@link Map} key or a {@link Collection} index. Indexes names are not
 * restricted and are considered case-sensitive.
 * <p>
 * Here are some typical examples:
 * <ul>
 * <li>{@code spring.main.banner-mode}</li>
 * <li>{@code server.hosts[0].name}</li>
 * <li>{@code log[org.springboot].level}</li>
 * </ul>
 * <p>
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see #of(CharSequence)
 * @see ConfigurationPropertySource
 */
public final class ConfigurationPropertyName
		implements Comparable<ConfigurationPropertyName> {

	public static final ConfigurationPropertyName EMPTY = new ConfigurationPropertyName(
			"", new String[0]);

	private final CharSequence name;

	private final CharSequence[] elements;

	private final CharSequence[] uniformElements;

	private int[] elementHashCodes;

	private ConfigurationPropertyName(CharSequence name, CharSequence[] elements) {
		this.name = name;
		this.elements = elements;
		this.uniformElements = new CharSequence[elements.length];
	}

	public boolean isLastElementIndexed() {
		int size = getNumberOfElements();
		return (size > 0 && isIndexed(this.elements[size - 1]));
	}

	boolean isIndexed(int elementIndex) {
		return isIndexed(this.elements[elementIndex]);
	}

	public CharSequence getLastElement(Form form) {
		int size = getNumberOfElements();
		return (size == 0 ? "" : getElement(size - 1, form));
	}

	public String getElement(int elementIndex, Form form) {
		if (form == Form.ORIGINAL) {
			CharSequence result = this.elements[elementIndex];
			if (isIndexed(result)) {
				result = result.subSequence(1, result.length() - 1);
			}
			return result.toString();
		}
		CharSequence result = this.uniformElements[elementIndex];
		if (result == null) {
			result = this.elements[elementIndex];
			if (isIndexed(result)) {
				result = result.subSequence(1, result.length() - 1);
			}
			else {
				result = stripDashes(result);
			}
			this.uniformElements[elementIndex] = result;
		}
		return result.toString();
	}

	private CharSequence stripDashes(CharSequence name) {
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) == '-') {
				// We save memory by only creating the new result if necessary
				StringBuilder result = new StringBuilder(name.length());
				result.append(name.subSequence(0, i));
				for (int j = i + 1; j < name.length(); j++) {
					char ch = name.charAt(j);
					if (ch != '-') {
						result.append(ch);
					}
				}
				return result;
			}
		}
		return name;
	}

	public int getNumberOfElements() {
		return this.elements.length;
	}

	public ConfigurationPropertyName append(String propertyName) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public int compareTo(ConfigurationPropertyName o) {
		throw new RuntimeException();
	}

	public boolean isParentOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (this.getNumberOfElements() != name.getNumberOfElements() - 1) {
			return false;
		}
		return isAncestorOf(name);
	}

	public boolean isAncestorOf(ConfigurationPropertyName name) {
		Assert.notNull(name, "Name must not be null");
		if (this.getNumberOfElements() >= name.getNumberOfElements()) {
			return false;
		}
		for (int i = 0; i < this.elements.length; i++) {
			if (!elementEquals(this.elements[i], name.elements[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return this.name.toString();
	}

	@Override
	public int hashCode() {
		if (this.elementHashCodes == null) {
			this.elementHashCodes = getElementHashCodes();
		}
		return ObjectUtils.nullSafeHashCode(this.elementHashCodes);
	}

	private int[] getElementHashCodes() {
		int[] hashCodes = new int[this.elements.length];
		for (int i = 0; i < this.elements.length; i++) {
			hashCodes[i] = getElementHashCode(this.elements[i]);
		}
		return hashCodes;
	}

	private int getElementHashCode(CharSequence element) {
		int hash = 0;
		int offset = (isIndexed(element) ? 1 : 0);
		for (int i = 0 + offset; i < element.length() - offset; i++) {
			char ch = element.charAt(i);
			hash = (ch == '-' ? hash : 31 * hash + Character.hashCode(ch));
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		ConfigurationPropertyName other = (ConfigurationPropertyName) obj;
		if (getNumberOfElements() != other.getNumberOfElements()) {
			return false;
		}
		for (int i = 0; i < this.elements.length; i++) {
			if (!elementEquals(this.elements[i], other.elements[i])) {
				return false;
			}
		}
		return true;
	}

	private boolean elementEquals(CharSequence e1, CharSequence e2) {
		int l1 = e1.length();
		int l2 = e2.length();
		int offset1 = (isIndexed(e1) ? 1 : 0);
		int offset2 = (isIndexed(e2) ? 1 : 0);
		int i1 = offset1;
		int i2 = offset2;
		while (i1 < l1 - offset1) {
			if (i2 >= l2 - offset2) {
				return false;
			}
			char ch1 = e1.charAt(i1);
			char ch2 = e2.charAt(i2);
			if (ch1 == '-') {
				i1++;
			}
			else if (ch2 == '-') {
				i2++;
			}
			else if (ch1 != ch2) {
				return false;
			}
			else {
				i1++;
				i2++;
			}
		}
		while (i2 < l2 - offset2) {
			if (e2.charAt(i2++) != '-') {
				return false;
			}
		}
		return true;
	}

	private boolean isIndexed(CharSequence element) {
		int length = element.length();
		return length > 2 && element.charAt(0) == '['
				&& element.charAt(length - 1) == ']';
	}

	/**
	 * Roll up the given name to the first element below the root. For example a name of
	 * {@code foo.bar.baz} rolled up to the root {@code foo} would be {@code foo.bar}.
	 * @param root the root name
	 * @return the rolled up name or {@code null}
	 */
	public final ConfigurationPropertyName rollUp(ConfigurationPropertyName root) {
		// FIXME very odd API
		throw new RuntimeException();
	}

	public static boolean isValid(CharSequence name) {
		return process(name, '.', ElementProcessor.NONE);
	}

	public static ConfigurationPropertyName of(CharSequence name) {
		Assert.notNull(name, "Name must not be null");
		if (name.length() == 0) {
			return EMPTY;
		}
		List<CharSequence> elements = new ArrayList<CharSequence>(10);
		boolean valid = process(name, '.', (start, end, indexed) -> {
			if (end > start) {
				elements.add(name.subSequence(start, end));
			}
		});
		Assert.isTrue(valid, name + " is not valid");
		return new ConfigurationPropertyName(name,
				elements.toArray(new CharSequence[elements.size()]));
	}

	public static ConfigurationPropertyName parse(CharSequence name, char separator) {
		throw new RuntimeException();
	}

	private static boolean process(CharSequence name, char separator,
			ElementProcessor elementProcessor) {
		int start = 0;
		boolean indexed = false;
		int length = name.length();
		for (int i = 0; i < length; i++) {
			char ch = name.charAt(i);
			if (indexed && ch == ']') {
				elementProcessor.process(start, i + 1, indexed);
				start = i + 1;
				indexed = false;
			}
			else if (!indexed && ch == '[') {
				elementProcessor.process(start, i, indexed);
				start = i;
				indexed = true;
			}
			else if (!indexed && ch == '.') {
				if (i == 0 || i == length - 1) {
					return false;
				}
				elementProcessor.process(start, i, indexed);
				start = i + 1;
			}
			else if (!indexed && !isValid(ch, i)) {
				return false;
			}
		}
		if (indexed) {
			return false;
		}
		for (int i = start; i < length; i++) {
			if (!isValid(name.charAt(i), i - start)) {
				return false;
			}
		}
		elementProcessor.process(start, length, false);
		return true;
	}

	static boolean isValid(char ch, int index) {
		boolean isAlpha = ch >= 'a' && ch <= 'z';
		boolean isNumeric = ch >= '0' && ch <= '9';
		if (index == 0) {
			return isAlpha;
		}
		return isAlpha || isNumeric || ch == '-';
	}

	/**
	 * The various forms that a non-indexed {@link Element} {@code value} can take.
	 */
	public enum Form {

		/**
		 * The original form as specified when the name was created. For example:
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foo-bar}"</li>
		 * <li>"{@code fooBar}" = "{@code fooBar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foo_bar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		ORIGINAL,

		/**
		 * The uniform configuration form (used for equals/hashcode; lower-case with only
		 * alphanumeric characters).
		 * <ul>
		 * <li>"{@code foo-bar}" = "{@code foobar}"</li>
		 * <li>"{@code fooBar}" = "{@code foobar}"</li>
		 * <li>"{@code foo_bar}" = "{@code foobar}"</li>
		 * <li>"{@code [Foo.bar]}" = "{@code Foo.bar}"</li>
		 * </ul>
		 */
		UNIFORM

	}

	@FunctionalInterface
	private static interface ElementProcessor {

		ElementProcessor NONE = (start, end, indexed) -> {
		};

		void process(int start, int end, boolean indexed);

	}

}
