/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.selector;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

/**
 * Simple {@link Labels} implementation.
 *
 * @author Phillip Webb
 * @param labels the actual labels
 */
record SimpleLabels(Map<String, Label> labels) implements Labels {

	static SimpleLabels NONE = new SimpleLabels(Collections.emptyMap());

	@Override
	public Label get(String key) throws NoSuchLabelKeyException {
		Label label = this.labels.get(key);
		if (label == null) {
			throw new NoSuchLabelKeyException(null, key, null);
		}
		return label;
	}

	@Override
	public boolean isEmpty() {
		return this.labels.isEmpty();
	}

	@Override
	public boolean contains(Label label) {
		return (label != null) && containsWithKnownKey(label.key(), (candidate) -> label.matches(candidate));
	}

	@Override
	public boolean contains(String key) {
		return this.labels.containsKey(key);
	}

	@Override
	public boolean contains(String key, String value) {
		return containsWithKnownKey(key, (candidate) -> candidate.matches(key, value));
	}

	@Override
	public boolean contains(String key, Predicate<String> predicate) {
		return containsWithKnownKey(key, (candidate) -> candidate.matches(key, predicate));
	}

	private boolean containsWithKnownKey(String key, Predicate<Label> predicate) {
		Label candidate = this.labels.get(key);
		return (candidate != null) && predicate.test(candidate);
	}

	@Override
	public Stream<Label> stream() {
		return this.labels.values().stream();
	}

	@Override
	public Iterator<Label> iterator() {
		return this.labels.values().iterator();
	}

	@Override
	public final String toString() {
		return "[" + StringUtils.collectionToCommaDelimitedString(this.labels.values()) + "]";
	}

	static Labels of(Stream<? extends Label> labels) {
		return new SimpleLabels(collectToMap(labels));
	}

	private static Map<String, Label> collectToMap(Stream<? extends Label> labels) {
		return labels.collect(Collector.of(LinkedHashMap::new, SimpleLabels::accumulate, SimpleLabels::combine,
				Collections::unmodifiableMap, Characteristics.IDENTITY_FINISH));
	}

	private static void accumulate(Map<String, Label> map, Label label) {
		SimpleLabel.validate(label);
		putIfAbsentAndCheckForDuplicate(map, label.key(), label);
	}

	private static Map<String, Label> combine(Map<String, Label> partialIn, Map<String, Label> partialOut) {
		partialOut.forEach((key, label) -> putIfAbsentAndCheckForDuplicate(partialIn, key, label));
		return partialIn;
	}

	private static void putIfAbsentAndCheckForDuplicate(Map<String, Label> map, String key, Label label) {
		Label duplicate = map.putIfAbsent(key, label);
		if (duplicate != null) {
			throw new DuplicateLabelKeyException(null, List.of(label, duplicate), null);
		}
	}

}
