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

package org.springframework.boot.test.mvc;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Date;

import org.assertj.core.api.AbstractByteArrayAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.MapAssert;

import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Assert;

/**
 * @author Phillip Webb
 */
public class MvcResultAssert extends AbstractObjectAssert<MvcResultAssert, MvcResult> {

	private Class<?> resourceLoadClass;

	public MvcResultAssert(Class<?> resourceLoadClass, MvcResult actual) {
		super(actual, MvcResultAssert.class);
		this.resourceLoadClass = resourceLoadClass;
	}

	public MvcResultAssert hasAsyncResult(Object expected) {
		extractingAsyncResult().isEqualTo(expected);
		return this;
	}

	public AbstractObjectAssert<?, Object> extractingAsyncResult() {
		hasStartedAsync();
		return Assertions.assertThat(getResult().getAsyncResult());
	}

	public MvcResultAssert hasStartedAsync() {
		matches(MockMvcResultMatchers.request().asyncStarted());
		return this;
	}

	public MvcResultAssert hasNotStartedAsync() {
		matches(MockMvcResultMatchers.request().asyncNotStarted());
		return this;
	}

	public MvcResultAssert containsRequestAttributeValue(String name, Object value) {
		extractingRequestAttribute(name).isEqualTo(value);
		return this;
	}

	public AbstractObjectAssert<?, Object> extractingRequestAttribute(String name) {
		return Assertions.assertThat(getRequest().getAttribute(name));
	}

	public MvcResultAssert containsSessionAttributeValue(String name, Object value) {
		extractingSessionAttribute(name).isEqualTo(value);
		return this;
	}

	public AbstractObjectAssert<?, Object> extractingSessionAttribute(String name) {
		return Assertions.assertThat(getRequest().getSession().getAttribute(name));
	}

	public MvcResultAssert wasProcessedBy(Class<?> type) {
		return matches(MockMvcResultMatchers.handler().handlerType(type));
	}

	public MvcResultAssert wasProcessedBy(Class<?> type, String methodName) {
		matches(MockMvcResultMatchers.handler().handlerType(type));
		return matches(MockMvcResultMatchers.handler().methodName(methodName));
	}

	public MvcResultAssert wasProcessedBy(Method method) {
		return matches(MockMvcResultMatchers.handler().method(method));
	}

	public MvcResultAssert containsModelAttributes(String... names) {
		extractingModel().containsKeys(names);
		return this;
	}

	public MvcResultAssert doesNotContainModelAttribute(String... names) {
		extractingModel().doesNotContainKeys(names);
		return this;
	}

	public MvcResultAssert containsModelAttributeValue(String attribute, Object value) {
		extractingModel().containsEntry(attribute, value);
		return this;
	}

	public MvcResultAssert hasModelErrors(String name, int expected) {
		extractingModel().hasErrors(name, expected);
		return this;
	}

	public MvcResultAssert hasModelErrors(String... names) {
		extractingModel().hasErrors(names);
		return this;
	}

	public MvcResultAssert hasNoModelErrors(String... names) {
		return null;
	}

	public ModelAssert extractingModel() {
		return new ModelAssert(getResult().getModelAndView().getModelMap());
	}

	public MvcResultAssert isRenderedWithView(String viewName) {
		return matches(MockMvcResultMatchers.view().name(viewName));
	}

	public MvcResultAssert containsFlashAttributes(String... names) {
		extractingFlashMap().containsKeys(names);
		return this;
	}

	public MvcResultAssert containsFlashAttributeValue(String name, Object value) {
		extractingRequestAttribute(name).isEqualTo(value);
		return this;
	}

	public MapAssert<String, Object> extractingFlashMap() {
		return Assertions.assertThat(getResult().getFlashMap());
	}

	public MvcResultAssert isForwardTo(String expectedUrl) {
		return matches(MockMvcResultMatchers.forwardedUrl(expectedUrl));
	}

	public MvcResultAssert isForwardMatching(String pattern) {
		return matches(MockMvcResultMatchers.forwardedUrlPattern(pattern));
	}

	public MvcResultAssert isRedirectTo(String url) {
		return matches(MockMvcResultMatchers.redirectedUrl(url));
	}

	public MvcResultAssert isRedirectMatching(String pattern) {
		return matches(MockMvcResultMatchers.redirectedUrlPattern(pattern));
	}

	public MvcResultAssert isOk() {
		return matches(MockMvcResultMatchers.status().isOk());
	}

	public MvcResultAssert isCreated() {
		return matches(MockMvcResultMatchers.status().isCreated());
	}

	public MvcResultAssert isNoContent() {
		return matches(MockMvcResultMatchers.status().isNoContent());
	}

	public MvcResultAssert isNotModified() {
		return matches(MockMvcResultMatchers.status().isNotModified());
	}

	public MvcResultAssert isBadRequest() {
		return matches(MockMvcResultMatchers.status().isBadRequest());
	}

	public MvcResultAssert isUnauthorized() {
		return matches(MockMvcResultMatchers.status().isUnauthorized());
	}

	public MvcResultAssert isForbidden() {
		return matches(MockMvcResultMatchers.status().isForbidden());
	}

	public MvcResultAssert isNotFound() {
		return matches(MockMvcResultMatchers.status().isNotFound());
	}

	public MvcResultAssert isGone() {
		return matches(MockMvcResultMatchers.status().isGone());
	}

	public MvcResultAssert isUnsupportedMediaType() {
		return matches(MockMvcResultMatchers.status().isUnsupportedMediaType());
	}

	public MvcResultAssert isUnprocessableEntity() {
		return matches(MockMvcResultMatchers.status().isUnprocessableEntity());
	}

	public MvcResultAssert isTooManyRequest() {
		return matches(MockMvcResultMatchers.status().isTooManyRequests());
	}

	public MvcResultAssert is(HttpStatus status) {
		Assert.notNull(status, "Status must not be null");
		return matches(MockMvcResultMatchers.status().is(status.value()));
	}

	public MvcResultAssert is(int status) {
		return matches(MockMvcResultMatchers.status().is(status));
	}

	public MvcResultAssert is1xxInformational() {
		return matches(MockMvcResultMatchers.status().is1xxInformational());
	}

	public MvcResultAssert is2xxSuccessful() {
		return matches(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	public MvcResultAssert is3xxRedirection() {
		return matches(MockMvcResultMatchers.status().is3xxRedirection());
	}

	public MvcResultAssert is4xxClientError() {
		return matches(MockMvcResultMatchers.status().is4xxClientError());
	}

	public MvcResultAssert is5xxServerError() {
		return matches(MockMvcResultMatchers.status().is5xxServerError());
	}

	public MvcResultAssert containsHeaders(String... names) {
		for (String name : names) {
			if (!getResponse().containsHeader(name)) {
				throw new AssertionError("Does not contain header " + name);
			}
		}
		return this;
	}

	public MvcResultAssert containsHeaderValue(String name, Object value) {
		if (value instanceof Long) {
			return matches(MockMvcResultMatchers.header().longValue(name, (Long) value));
		}
		if (value instanceof Date) {
			return matches(MockMvcResultMatchers.header().dateValue(name,
					((Date) value).getTime()));
		}
		extractingHeader(name).isEqualTo(value);
		return null;
	}

	public AbstractCharSequenceAssert<?, String> extractingHeader(String name) {
		return Assertions.assertThat(getResponse().getHeader(name));
	}

	public MvcResultAssert isContentType(MediaType contentType) {
		return matches(MockMvcResultMatchers.content().contentType(contentType));
	}

	public MvcResultAssert isCompatibleWithContentType(MediaType contentType) {
		ResultMatcher matcher = MockMvcResultMatchers.content()
				.contentTypeCompatibleWith(contentType);
		return matches(matcher);
	}

	public MvcResultAssert hasCharacterEncoding(String characterEncoding) {
		return matches(MockMvcResultMatchers.content().encoding(characterEncoding));
	}

	public MvcResultAssert contains(byte... values) {
		asByteArray().contains(values);
		return this;
	}

	public MvcResultAssert contains(CharSequence... values) {
		asString().contains(values);
		return this;
	}

	public MvcResultAssert isEqualToJson(String expected) {
		asJson().isEqualToJson(expected);
		return this;
	}

	@Override
	public AbstractCharSequenceAssert<?, String> asString() {
		return Assertions.assertThat(getContentAsString());
	}

	public AbstractByteArrayAssert<?> asByteArray() {
		return Assertions.assertThat(getContentAsByteArray());
	}

	public JsonContentAssert asJson() {
		return new JsonContentAssert(this.resourceLoadClass, getContentAsString());
	}

	// Cookie

	public MvcResultAssert containsCookieValue(String name, Object value) {
		extractingCookie(name).isEqualTo(value);
		return this;
	}

	public MvcResultAssert containsCookie(String... names) {
		for (String name : names) {
			matches(MockMvcResultMatchers.cookie().exists(name));
		}
		return this;
	}

	public MvcResultAssert doesNotContainCookie(String... names) {
		for (String name : names) {
			matches(MockMvcResultMatchers.cookie().doesNotExist(name));
		}
		return this;
	}

	public CookieAssert extractingCookie(String name) {
		return new CookieAssert(getResponse().getCookie(name));
	}

	public MvcResultAssert matches(ResultMatcher matcher) {
		try {
			matcher.match(getResult());
			return this.myself;
		}
		catch (Throwable ex) {
			if (ex instanceof AssertionError) {
				throw (AssertionError) ex;
			}
			throw new AssertionError(ex);
		}
	}

	private String getContentAsString() {
		try {
			return getResult().getResponse().getContentAsString();
		}
		catch (UnsupportedEncodingException ex) {
			throw new AssertionError(ex);
		}
	}

	private byte[] getContentAsByteArray() {
		return getResult().getResponse().getContentAsByteArray();
	}

	private MockHttpServletRequest getRequest() {
		return getResult().getRequest();
	}

	private MockHttpServletResponse getResponse() {
		return getResult().getResponse();
	}

	private MvcResult getResult() {
		return this.actual;
	}

}
