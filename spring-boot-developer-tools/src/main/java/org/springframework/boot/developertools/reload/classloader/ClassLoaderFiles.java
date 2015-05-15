/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.reload.classloader;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.loading.ClassLoaderRepository;

import org.springframework.util.Assert;

/**
 * {@link ClassLoaderFileRepository} that maintains a collection of
 * {@link ClassLoaderFile} items.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassLoaderFile
 * @see ClassLoaderRepository
 */
public class ClassLoaderFiles implements ClassLoaderFileRepository {

	private final Map<String, ClassLoaderFile> files = new LinkedHashMap<String, ClassLoaderFile>();

	public void addFile(String name, ClassLoaderFile file) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(file, "File must not be null");
		this.files.put(name, file);
	}

	@Override
	public ClassLoaderFile getFile(String name) {
		return this.files.get(name);
	}

}
