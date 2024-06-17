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

package org.springframework.boot.logging.log4j2;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.logging.json.Field;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author Moritz Halbritter
 */
@Plugin(name = "SpringJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class JsonLayout extends AbstractStringLayout {

	private final Log4jJsonFormat format;

	public JsonLayout(Log4jJsonFormat format) {
		super(StandardCharsets.UTF_8);
		Assert.notNull(format, "Format must not be null");
		this.format = format;
	}

	@Override
	public String toSerializable(LogEvent event) {
		StringBuilder output = getStringBuilder();
		output.append('{');
		boolean appendedComma = false;
		for (Field field : this.format.getFields(event)) {
			field.write(output);
			output.append(',');
			appendedComma = true;
		}
		if (appendedComma) {
			removeTrailingComma(output);
		}
		output.append('}');
		output.append('\n');
		return output.toString();
	}

	private void removeTrailingComma(StringBuilder output) {
		output.setLength(output.length() - 1);
	}

	@PluginBuilderFactory
	static JsonLayout.Builder newBuilder() {
		return new JsonLayout.Builder();
	}

	static final class Builder implements org.apache.logging.log4j.core.util.Builder<JsonLayout> {

		@PluginBuilderAttribute
		private String format;

		@PluginBuilderAttribute
		private Long pid;

		@PluginBuilderAttribute
		private String serviceName;

		@PluginBuilderAttribute
		private String serviceVersion;

		@PluginBuilderAttribute
		private String serviceNodeName;

		@PluginBuilderAttribute
		private String serviceEnvironment;

		@Override
		public JsonLayout build() {
			Log4jJsonFormat format = createFormat();
			format.setPid(this.pid);
			format.setServiceName(this.serviceName);
			format.setServiceVersion(this.serviceVersion);
			format.setServiceNodeName(this.serviceNodeName);
			format.setServiceEnvironment(this.serviceEnvironment);
			return new JsonLayout(format);
		}

		private Log4jJsonFormat createFormat() {
			Log4jJsonFormat commonFormat = CommonJsonFormats.create(this.format);
			if (commonFormat != null) {
				return commonFormat;
			}
			else if (ClassUtils.isPresent(this.format, null)) {
				return BeanUtils.instantiateClass(ClassUtils.resolveClassName(this.format, null),
						Log4jJsonFormat.class);
			}
			else {
				throw new IllegalArgumentException("Unknown format '%s'. Common formats are: %s".formatted(this.format,
						CommonJsonFormats.names()));
			}
		}

	}

}
