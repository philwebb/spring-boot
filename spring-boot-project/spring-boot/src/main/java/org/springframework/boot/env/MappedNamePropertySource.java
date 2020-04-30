/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.env;

import java.util.Arrays;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * @author Phillip Webb
 */
class MappedNamePropertySource {

	static <T> PropertySource<T> get(PropertySource<T> propertySource, PropertyNameMapper propertyNameMapper) {
		if (propertySource instanceof EnumerablePropertySource<?>) {
			return get((EnumerablePropertySource<T>) propertySource, propertyNameMapper);
		}
		return new NameMappingPropertySource<>(propertySource, propertyNameMapper);
	}

	static <T> EnumerablePropertySource<T> get(EnumerablePropertySource<T> propertySource,
			PropertyNameMapper propertyNameMapper) {
		return new NameMappingEnumerablePropertySource<>(propertySource, propertyNameMapper);
	}

	private static class NameMappingPropertySource<T> extends PropertySource<T> implements OriginLookup<String> {

		private final PropertySource<T> delegate;

		private final PropertyNameMapper propertyNameMapper;

		private NameMappingPropertySource(PropertySource<T> delegate, PropertyNameMapper propertyNameMapper) {
			super(delegate.getName(), delegate.getSource());
			this.delegate = delegate;
			this.propertyNameMapper = propertyNameMapper;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Origin getOrigin(String name) {
			if (this.delegate instanceof OriginLookup) {
				String mappedName = this.propertyNameMapper.mapTo(name);
				if (mappedName != null) {
					return ((OriginLookup<String>) this.delegate).getOrigin(mappedName);
				}
			}
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isImmutable() {
			if (this.delegate instanceof OriginLookup) {
				return ((OriginLookup<String>) this.delegate).isImmutable();
			}
			return false;
		}

		@Override
		public Object getProperty(String name) {
			String mappedName = this.propertyNameMapper.mapTo(name);
			if (mappedName != null) {
				return this.delegate.getProperty(mappedName);
			}
			return null;
		}

	}

	private static class NameMappingEnumerablePropertySource<T> extends EnumerablePropertySource<T>
			implements OriginLookup<String> {

		private final EnumerablePropertySource<T> delegate;

		private final PropertyNameMapper propertyNameMapper;

		private NameMappingEnumerablePropertySource(EnumerablePropertySource<T> delegate,
				PropertyNameMapper propertyNameMapper) {
			super(delegate.getName(), delegate.getSource());
			this.delegate = delegate;
			this.propertyNameMapper = propertyNameMapper;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Origin getOrigin(String name) {
			if (this.delegate instanceof OriginLookup) {
				String mappedName = this.propertyNameMapper.mapTo(name);
				if (mappedName != null) {
					return ((OriginLookup<String>) this.delegate).getOrigin(mappedName);
				}
			}
			return null;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isImmutable() {
			if (this.delegate instanceof OriginLookup) {
				return ((OriginLookup<String>) this.delegate).isImmutable();
			}
			return false;
		}

		@Override
		public String[] getPropertyNames() {
			return Arrays.stream(this.delegate.getPropertyNames()).map(this.propertyNameMapper::mapFrom)
					.toArray(String[]::new);
		}

		@Override
		public Object getProperty(String name) {
			String mappedName = this.propertyNameMapper.mapTo(name);
			if (mappedName != null) {
				return this.delegate.getProperty(mappedName);
			}
			return null;
		}

	}

	interface PropertyNameMapper {

		String mapTo(String name);

		String mapFrom(String name);

		static PropertyNameMapper prefixMapper(String prefix) {
			return new PropertyNameMapper() {

				@Override
				public String mapTo(String name) {
					return prefix + name;
				}

				@Override
				public String mapFrom(String name) {
					return (name.startsWith(prefix) ? name.substring(prefix.length()) : null);
				}
			};
		}

	}

}
