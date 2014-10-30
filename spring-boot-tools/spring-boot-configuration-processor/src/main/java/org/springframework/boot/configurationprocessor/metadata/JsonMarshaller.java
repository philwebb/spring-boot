/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshaller to write {@link ConfigurationMetadata} as JSON.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
public class JsonMarshaller {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final int BUFFER_SIZE = 4098;

	public void write(ConfigurationMetadata metadata, OutputStream outputStream)
			throws IOException {
		JSONObject object = new JSONObject();
		object.put("items", toJsonArray(metadata));
		outputStream.write(object.toString(2).getBytes(UTF_8));
	}

	private JSONArray toJsonArray(ConfigurationMetadata metadata) {
		JSONArray items = new JSONArray();
		for (PropertyMetadata propertyMetadata : metadata.getProperties()) {
			items.put(toJsonObject(propertyMetadata));
		}
		return items;
	}

	private JSONObject toJsonObject(PropertyMetadata propertyMetadata) {
		JSONObject item = new JSONOrderedObject();
		item.put("name", propertyMetadata.getName());
		putIfPresent(item, "dataType", propertyMetadata.getDataType());
		putIfPresent(item, "sourceType", propertyMetadata.getSourceType());
		putIfPresent(item, "sourceMethod", propertyMetadata.getSourceMethod());
		putIfPresent(item, "description", propertyMetadata.getDescription());
		putIfPresent(item, "defaultValue", propertyMetadata.getDefaultValue());
		return item;
	}

	private void putIfPresent(JSONObject item, String name, Object value) {
		if (value != null) {
			item.put(name, value);
		}
	}

	public ConfigurationMetadata read(InputStream inputStream) throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = new JSONObject(toString(inputStream));
		JSONArray items = (JSONArray) object.get("items");
		for (int i = 0; i < items.length(); i++) {
			metadata.add(toPropertyMetadata((JSONObject) items.get(i)));
		}
		return metadata;
	}

	private PropertyMetadata toPropertyMetadata(JSONObject object) {
		String name = object.getString("name");
		String dataType = object.optString("dataType", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		String description = object.optString("description", null);
		Object defaultValue = object.opt("defaultValue");
		return new PropertyMetadata(name, dataType, sourceType, sourceMethod,
				description, defaultValue);
	}

	private String toString(InputStream inputStream) throws IOException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

	/**
	 * Extension to {@link JSONObject} that remembers the order of inserts.
	 */
	@SuppressWarnings("rawtypes")
	private static class JSONOrderedObject extends JSONObject {

		private Set<String> keys = new LinkedHashSet<String>();

		@Override
		public JSONObject put(String key, Object value) throws JSONException {
			this.keys.add(key);
			return super.put(key, value);
		}

		@Override
		public Set keySet() {
			return this.keys;
		}

	}

}
