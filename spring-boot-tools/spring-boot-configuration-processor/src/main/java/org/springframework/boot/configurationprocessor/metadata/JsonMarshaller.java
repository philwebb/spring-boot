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
		object.put("groups", toJsonArray(metadata, GroupMetadata.class));
		object.put("properties", toJsonArray(metadata, PropertyMetadata.class));
		outputStream.write(object.toString(2).getBytes(UTF_8));
	}

	private JSONArray toJsonArray(ConfigurationMetadata metadata,
			Class<? extends ItemMetadata> itemType) {
		JSONArray jsonArray = new JSONArray();
		for (ItemMetadata itemMetadata : metadata.getItems()) {
			if (itemType.isInstance(itemMetadata)) {
				jsonArray.put(toJsonObject(itemMetadata));
			}
		}
		return jsonArray;
	}

	private JSONObject toJsonObject(ItemMetadata itemMetadata) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", itemMetadata.getName());
		putIfPresent(jsonObject, "sourceType", itemMetadata.getSourceType());
		if (itemMetadata instanceof PropertyMetadata) {
			putIfPresent(jsonObject, "dataType",
					((PropertyMetadata) itemMetadata).getDataType());
		}
		putIfPresent(jsonObject, "sourceMethod", itemMetadata.getSourceMethod());
		putIfPresent(jsonObject, "description", itemMetadata.getDescription());
		return jsonObject;
	}

	private void putIfPresent(JSONObject item, String name, Object value) {
		if (value != null) {
			item.put(name, value);
		}
	}

	public ConfigurationMetadata read(InputStream inputStream) throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = new JSONObject(toString(inputStream));
		JSONArray groups = object.optJSONArray("groups");
		if (groups != null) {
			for (int i = 0; i < groups.length(); i++) {
				metadata.add(toGroupMetadata((JSONObject) groups.get(i)));
			}
		}
		JSONArray properties = object.optJSONArray("properties");
		if (properties != null) {
			for (int i = 0; i < properties.length(); i++) {
				metadata.add(toPropertyMetadata((JSONObject) properties.get(i)));
			}
		}
		return metadata;
	}

	private GroupMetadata toGroupMetadata(JSONObject object) {
		String name = object.getString("name");
		String dataType = object.optString("dataType", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		String description = object.optString("description", null);
		return new GroupMetadata(name, dataType, sourceType, sourceMethod, description);
	}

	private PropertyMetadata toPropertyMetadata(JSONObject object) {
		String name = object.getString("name");
		String dataType = object.optString("dataType", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		String description = object.optString("description", null);
		return new PropertyMetadata(name, dataType, sourceType, sourceMethod, description);
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
