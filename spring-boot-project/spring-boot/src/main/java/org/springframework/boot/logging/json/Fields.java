/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Collection of {@link Field fields}.
 *
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public class Fields implements Iterable<Field> {

	private final List<Field> fields = new ArrayList<>();

	/**
	 * Adds the given field.
	 * @param field the field to add
	 */
	public void add(Field field) {
		this.fields.add(field);
	}

	/**
	 * Adds the given key and value.
	 * @param key the key to add
	 * @param value the value to add
	 */
	public void add(Key key, Value value) {
		this.fields.add(Field.of(key, value));
	}

	/**
	 * Adds all given fields.
	 * @param fields the fields to add
	 */
	public void addAll(Iterable<? extends Field> fields) {
		for (Field field : fields) {
			this.fields.add(field);
		}
	}

	/**
	 * Adds all given fields.
	 * @param fields the fields to add
	 */
	public void addAll(Field... fields) {
		this.fields.addAll(Arrays.asList(fields));
	}

	@Override
	public Iterator<Field> iterator() {
		return this.fields.iterator();
	}

	/**
	 * Creates a new instance with the given fields
	 * @param fields the fields
	 * @return the new instance
	 */
	public static Fields of(Iterable<? extends Field> fields) {
		Fields result = new Fields();
		result.addAll(fields);
		return result;
	}

	/**
	 * Creates a new instance with the given fields.
	 * @param fields the fields
	 * @return the new instance
	 */
	public static Fields of(Field... fields) {
		Fields result = new Fields();
		result.addAll(fields);
		return result;
	}

}
