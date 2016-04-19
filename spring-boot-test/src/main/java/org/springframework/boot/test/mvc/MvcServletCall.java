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

import java.security.Principal;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;

import org.assertj.core.api.AssertProvider;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;

/**
 * @author pwebb
 */
public class MvcServletCall implements AssertProvider<MvcResultAssert> {

	private final MockMvc mvc;

	private final MockHttpServletRequestBuilder builder;

	MvcServletCall(MockMvc mvc, MockHttpServletRequestBuilder builder) {
		this.mvc = mvc;
		this.builder = builder;
	}

	/**
	 * Add a request parameter to the {@link MockHttpServletRequest}.
	 * <p>
	 * If called more than once, new values get added to existing ones.
	 * @param name the parameter name
	 * @param values one or more values
	 * @return this instance
	 */
	public MvcServletCall param(String name, String... values) {
		this.builder.param(name, values);
		return this;
	}

	/**
	 * Add a map of request parameters to the {@link MockHttpServletRequest}, for example
	 * when testing a form submission.
	 * <p>
	 * If called more than once, new values get added to existing ones.
	 * @param params the parameters to add
	 * @return this instance
	 */
	public MvcServletCall params(MultiValueMap<String, String> params) {
		this.builder.params(params);
		return this;
	}

	/**
	 * Add a header to the request. Values are always added.
	 * @param name the header name
	 * @param values one or more header values
	 * @return this instance
	 */
	public MvcServletCall header(String name, Object... values) {
		this.builder.header(name, values);
		return this;
	}

	/**
	 * Add all headers to the request. Values are always added.
	 * @param httpHeaders the headers and values to add
	 * @return this instance
	 */
	public MvcServletCall headers(HttpHeaders httpHeaders) {
		this.builder.headers(httpHeaders);
		return this;
	}

	/**
	 * Set the 'Content-Type' header of the request.
	 * @param contentType the content type
	 * @return this instance
	 */
	public MvcServletCall contentType(MediaType contentType) {
		this.builder.contentType(contentType);
		return this;
	}

	/**
	 * Set the 'Content-Type' header of the request.
	 * @param contentType the content type
	 * @return this instance
	 */
	public MvcServletCall contentType(String contentType) {
		this.builder.contentType(contentType);
		return this;
	}

	/**
	 * Set the 'Accept' header to the given media type(s).
	 * @param mediaTypes one or more media types
	 * @return this instance
	 */
	public MvcServletCall accept(MediaType... mediaTypes) {
		this.builder.accept(mediaTypes);
		return this;
	}

	/**
	 * Set the 'Accept' header to the given media type(s).
	 * @param mediaTypes one or more media types
	 * @return this instance
	 */
	public MvcServletCall accept(String... mediaTypes) {
		this.builder.accept(mediaTypes);
		return this;
	}

	/**
	 * Set the request body.
	 * @param content the body content
	 * @return this instance
	 */
	public MvcServletCall content(byte[] content) {
		this.builder.content(content);
		return this;
	}

	/**
	 * Set the request body as a UTF-8 String.
	 * @param content the body content
	 * @return this instance
	 */
	public MvcServletCall content(String content) {
		this.builder.content(content);
		return this;
	}

	/**
	 * Add the given cookies to the request. Cookies are always added.
	 * @param cookies the cookies to add
	 * @return this instance
	 */
	public MvcServletCall cookie(Cookie... cookies) {
		this.builder.cookie(cookies);
		return this;
	}

	/**
	 * Set the locale of the request.
	 * @param locale the locale
	 * @return this instance
	 */
	public MvcServletCall locale(Locale locale) {
		this.builder.locale(locale);
		return this;
	}

	/**
	 * Set the character encoding of the request.
	 * @param encoding the character encoding
	 * @return this instance
	 */
	public MvcServletCall characterEncoding(String encoding) {
		this.builder.characterEncoding(encoding);
		return this;
	}

	/**
	 * Set a request attribute.
	 * @param name the attribute name
	 * @param value the attribute value
	 * @return this instance
	 */
	public MvcServletCall requestAttr(String name, Object value) {
		this.builder.requestAttr(name, value);
		return this;
	}

	/**
	 * Set a session attribute.
	 * @param name the session attribute name
	 * @param value the session attribute value
	 * @return this instance
	 */
	public MvcServletCall sessionAttr(String name, Object value) {
		this.builder.sessionAttr(name, value);
		return this;
	}

	/**
	 * Set session attributes.
	 * @param sessionAttributes the session attributes
	 * @return this instance
	 */
	public MvcServletCall sessionAttrs(Map<String, Object> sessionAttributes) {
		this.builder.sessionAttrs(sessionAttributes);
		return this;
	}

	/**
	 * Set an "input" flash attribute.
	 * @param name the flash attribute name
	 * @param value the flash attribute value
	 * @return this instance
	 */
	public MvcServletCall flashAttr(String name, Object value) {
		this.builder.flashAttr(name, value);
		return this;
	}

	/**
	 * Set flash attributes.
	 * @param flashAttributes the flash attributes
	 * @return this instance
	 */
	public MvcServletCall flashAttrs(Map<String, Object> flashAttributes) {
		this.builder.flashAttrs(flashAttributes);
		return this;
	}

	/**
	 * Set the HTTP session to use, possibly re-used across requests.
	 * <p>
	 * Individual attributes provided via {@link #sessionAttr(String, Object)} override
	 * the content of the session provided here.
	 * @param session the HTTP session
	 * @return this instance
	 */
	public MvcServletCall session(MockHttpSession session) {
		this.builder.session(session);
		return this;
	}

	/**
	 * Set the principal of the request.
	 * @param principal the principal
	 * @return this instance
	 */
	public MvcServletCall principal(Principal principal) {
		this.builder.principal(principal);
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the context path. The context
	 * path, if specified, must match to the start of the request URI.
	 * <p>
	 * In most cases, tests can be written by omitting the context path from the
	 * requestURI. This is because most applications don't actually depend on the name
	 * under which they're deployed. If specified here, the context path must start with a
	 * "/" and must not end with a "/".
	 * @param contextPath the context path
	 * @return this instance
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getContextPath%28%29">
	 * HttpServletRequest.getContextPath()</a>
	 */
	public MvcServletCall contextPath(String contextPath) {
		this.builder.contextPath(contextPath);
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the path to which the Servlet
	 * is mapped. This is typically a portion of the requestURI after the context path.
	 * <p>
	 * In most cases, tests can be written by omitting the servlet path from the
	 * requestURI. This is because most applications don't actually depend on the prefix
	 * to which a servlet is mapped. For example if a Servlet is mapped to
	 * {@code "/main/*"}, tests can be written with the requestURI {@code "/accounts/1"}
	 * as opposed to {@code "/main/accounts/1"}. If specified here, the servletPath must
	 * start with a "/" and must not end with a "/".
	 * @param servletPath the servlet path
	 * @return this instance
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getServletPath%28%29">
	 * HttpServletRequest.getServletPath()</a>
	 */
	public MvcServletCall servletPath(String servletPath) {
		this.builder.servletPath(servletPath);
		return this;
	}

	/**
	 * Specify the portion of the requestURI that represents the pathInfo.
	 * <p>
	 * If left unspecified (recommended), the pathInfo will be automatically derived by
	 * removing the contextPath and the servletPath from the requestURI and using any
	 * remaining part. If specified here, the pathInfo must start with a "/".
	 * <p>
	 * If specified, the pathInfo will be used as is.
	 * @param pathInfo the path info
	 * @return this instance
	 * @see <a
	 * href="http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getPathInfo%28%29">
	 * HttpServletRequest.getServletPath()</a>
	 */
	public MvcServletCall pathInfo(String pathInfo) {
		this.builder.pathInfo(pathInfo);
		return this;
	}

	/**
	 * Set the secure property of the {@link ServletRequest} indicating use of a secure
	 * channel, such as HTTPS.
	 * @param secure whether the request is using a secure channel
	 * @return this instance
	 */
	public MvcServletCall secure(boolean secure) {
		this.builder.secure(secure);
		return this;
	}

	@Override
	public MvcResultAssert assertThat() {
		return perform().assertThat();
	}

	public MvcOutcome perform() {
		try {
			return new MvcOutcome(this.mvc.perform(this.builder).andReturn());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
