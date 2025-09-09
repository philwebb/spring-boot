/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.autoconfigure;

import java.net.URI;
import java.net.URISyntaxException;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices.GroupProvider;

/**
 * {@link GroupProvider} used to support {@link HttpExchange @HttpExchange} annotations
 * that specify a URL. A group is provided when an {@link HttpExchange @HttpExchange}
 * annotation is specified on the class with an absolute URL, or a {@code group://} URL.
 * All other HTTP Service interfaces are filtered.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class HttpExchangeUrlsGroupProvider implements GroupProvider {

	public static final String GROUP_SCHEME = "clientservicegroup://";

	@Override
	public @Nullable String group(AnnotationMetadata metadata) {
		MergedAnnotation<?> httpExchange = metadata.getAnnotations().get(HttpExchange.class);
		String url = httpExchange.getValue("url", String.class).orElse("");
		if (url.startsWith(GROUP_SCHEME)) {
			return url.substring(GROUP_SCHEME.length());
		}
		if (isAbsoluteUrl(url)) {
			return HttpServiceGroup.DEFAULT_GROUP_NAME;
		}
		return null;
	}

	private boolean isAbsoluteUrl(String url) {
		try {
			return new URI(url).isAbsolute();
		}
		catch (URISyntaxException e) {
			return false;
		}
	}

}
