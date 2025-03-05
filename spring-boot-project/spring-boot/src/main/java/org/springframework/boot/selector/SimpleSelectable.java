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

import org.springframework.util.Assert;

/**
 * Simple {@link Selectable} implementation.
 *
 * @author Phillip Webb
 * @param name the name of the selectable
 * @param labels the labels associated with the selectable
 */
record SimpleSelectable(String name, Labels labels) implements SelectablePredicate {

	static final SimpleSelectable ANONYMOUS = new SimpleSelectable(null, null);

	SimpleSelectable {
		name = (name != null) ? name : "";
		labels = (labels != null) ? labels : Labels.NONE;
	}

	@Override
	public final String toString() {
		return toString(this);
	}

	static String toString(Selectable selectable) {
		return selectable.name() + ((!selectable.labels().isEmpty()) ? " " + selectable.labels() : "");
	}

	static SimpleSelectable of(String name, Labels labels) {
		Assert.hasText(name, "'name' must not be empty");
		return new SimpleSelectable(name, labels);
	}

}
