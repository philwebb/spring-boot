/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.mongo;

import com.mongodb.MongoClientSettings;

import org.springframework.core.Ordered;

/**
 * A {@link MongoClientSettingsBuilderCustomizer} that applies properties from a
 * {@link MongoProperties} to a {@link MongoClientSettings}.
 *
 * @author Scott Frederick
 * @author Safeer Ansari
 * @author Moritz Halbritter
 * @since 2.4.0
 */
public class MongoPropertiesClientSettingsBuilderCustomizer implements MongoClientSettingsBuilderCustomizer, Ordered {

	private final MongoProperties properties;

	private final MongoConnectionDetails connectionDetails;

	private int order = 0;

	// TODO Breaking API change due to addition of MongoConnectionDetails
	public MongoPropertiesClientSettingsBuilderCustomizer(MongoProperties properties,
			MongoConnectionDetails connectionDetails) {
		this.properties = properties;
		this.connectionDetails = connectionDetails;
	}

	@Override
	public void customize(MongoClientSettings.Builder settingsBuilder) {
		applyUuidRepresentation(settingsBuilder);
		applyHostAndPort(settingsBuilder);
	}

	private void applyUuidRepresentation(MongoClientSettings.Builder settingsBuilder) {
		settingsBuilder.uuidRepresentation(this.properties.getUuidRepresentation());
	}

	private void applyHostAndPort(MongoClientSettings.Builder settings) {
		settings.applyConnectionString(this.connectionDetails.getConnectionString());
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the order value of this object.
	 * @param order the new order value
	 * @see #getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

}
