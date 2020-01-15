/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link Layers} that uses implicit rules.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ImplicitLayerResolver implements Layers {

	private static final Layer DEPENDENCIES_LAYER = new Layer("dependencies");

	private static final Layer SNAPSHOT_DEPENDENCIES_LAYER = new Layer("snapshot-dependencies");

	private static final Layer RESOURCES_LAYER = new Layer("resources");

	private static final Layer APPLICATION_LAYER = new Layer("application");

	private static final List<Layer> LAYERS;
	static {
		List<Layer> layers = new ArrayList<>();
		layers.add(DEPENDENCIES_LAYER);
		layers.add(SNAPSHOT_DEPENDENCIES_LAYER);
		layers.add(RESOURCES_LAYER);
		layers.add(APPLICATION_LAYER);
		LAYERS = Collections.unmodifiableList(layers);
	}

	private static final String[] RESOURCE_LOCATIONS = { "META-INF/resources/", "resources/", "static/", "public/" };

	@Override
	public Iterator<Layer> iterator() {
		return LAYERS.iterator();
	}

	@Override
	public Layer getLayer(String name) {
		if (!isClassFile(name) && isInResourceLocation(name)) {
			return RESOURCES_LAYER;
		}
		return APPLICATION_LAYER;
	}

	@Override
	public Layer getLayer(Library library) {
		if (library.getName().contains("SNAPSHOT.")) {
			return SNAPSHOT_DEPENDENCIES_LAYER;
		}
		return DEPENDENCIES_LAYER;
	}

	private boolean isClassFile(String name) {
		return name.endsWith(".class");
	}

	private boolean isInResourceLocation(String name) {
		for (String resourceLocation : RESOURCE_LOCATIONS) {
			if (name.startsWith(resourceLocation)) {
				return true;
			}
		}
		return false;
	}

}
