/*
 * Copyright 2012-2016 the original author or authors.
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

package sample.test.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Details of a single vehicle.
 *
 * @author Phillip Webb
 */
public class VehicleDetails {

	private final String make;

	private final String model;

	@JsonCreator
	public VehicleDetails(@JsonProperty("make") String make,
			@JsonProperty("model") String model) {
		this.make = make;
		this.model = model;
	}

	public String getMake() {
		return this.make;
	}

	public String getModel() {
		return this.model;
	}

}
