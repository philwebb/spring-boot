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

package org.springframework.boot.json;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.NameProcessor;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.util.LambdaSafe;

/**
 * Internal record used to hold {@link NameProcessor} and {@link ValueProcessor}
 * instances.
 *
 * @author Phillip Webb
 * @param nameProcessors the name processors
 * @param valueProcessors the value processors
 */
record JsonProcessors(List<NameProcessor> nameProcessors, List<ValueProcessor<?>> valueProcessors) {

	JsonProcessors() {
		this(new ArrayList<>(), new ArrayList<>());
	}

	String processName(MemberPath path, String name) {
		String processed = name;
		for (NameProcessor nameProcessor : this.nameProcessors) {
			processed = (processed != null) ? nameProcessor.processName(path, processed) : processed;
		}
		return processed;
	}

	@SuppressWarnings("unchecked")
	<V> V processValue(MemberPath path, V value) {
		V processed = value;
		for (ValueProcessor<?> valueProcessor : this.valueProcessors) {
			processed = (V) LambdaSafe.callback(ValueProcessor.class, valueProcessor, path, value)
				.invokeAnd((call) -> call.processValue(path, value))
				.get(processed);
		}
		return processed;
	}

}
