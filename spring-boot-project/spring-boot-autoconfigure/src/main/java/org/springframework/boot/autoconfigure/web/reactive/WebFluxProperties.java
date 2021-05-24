/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties properties} for Spring WebFlux.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.webflux")
public class WebFluxProperties {

	/**
	 * Base path for all web handlers.
	 */
	private String basePath;

	private final Format format = new Format();

	private final Session session = new Session();

	/**
	 * Path pattern used for static resources.
	 */
	private String staticPathPattern = "/**";

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = cleanBasePath(basePath);
	}

	private String cleanBasePath(String basePath) {
		String candidate = StringUtils.trimWhitespace(basePath);
		if (StringUtils.hasText(candidate)) {
			if (!candidate.startsWith("/")) {
				candidate = "/" + candidate;
			}
			if (candidate.endsWith("/")) {
				candidate = candidate.substring(0, candidate.length() - 1);
			}
		}
		return candidate;
	}

	public Format getFormat() {
		return this.format;
	}

	public Session getSession() {
		return this.session;
	}

	public String getStaticPathPattern() {
		return this.staticPathPattern;
	}

	public void setStaticPathPattern(String staticPathPattern) {
		this.staticPathPattern = staticPathPattern;
	}

	public static class Format {

		/**
		 * Date format to use, for example `dd/MM/yyyy`.
		 */
		private String date;

		/**
		 * Time format to use, for example `HH:mm:ss`.
		 */
		private String time;

		/**
		 * Date-time format to use, for example `yyyy-MM-dd HH:mm:ss`.
		 */
		private String dateTime;

		public String getDate() {
			return this.date;
		}

		public void setDate(String date) {
			this.date = date;
		}

		public String getTime() {
			return this.time;
		}

		public void setTime(String time) {
			this.time = time;
		}

		public String getDateTime() {
			return this.dateTime;
		}

		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}

	}

	public static class Session {

		/**
		 * Session timeout. If a duration suffix is not specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration timeout = Duration.ofMinutes(30);

		private final Cookie cookie = new Cookie();

		public Cookie getCookie() {
			return this.cookie;
		}

		public Duration getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

	}

	public static class Cookie {

		private static final String COOKIE_NAME = "SESSION";

		/**
		 * Name attribute value for session Cookies.
		 */
		private String name = COOKIE_NAME;

		/**
		 * Domain attribute value for session Cookies.
		 */
		private String domain;

		/**
		 * Path attribute value for session Cookies.
		 */
		private String path;

		/**
		 * Maximum age of the session cookie. If a duration suffix is not specified,
		 * seconds will be used. A positive value indicates when the cookie expires
		 * relative to the current time. A value of 0 means the cookie should expire
		 * immediately. A negative value means no "Max-Age" attribute in which case the
		 * cookie is removed when the browser is closed.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration maxAge = Duration.ofSeconds(-1);

		/**
		 * HttpOnly attribute value for session Cookies.
		 */
		private Boolean httpOnly = true;

		/**
		 * Secure attribute value for session Cookies.
		 */
		private Boolean secure;

		/**
		 * SameSite attribute value for session Cookies.
		 */
		private SameSite sameSite = SameSite.LAX;

		/**
		 * Return the session cookie name.
		 * @return the session cookie name
		 */
		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Return the domain for the session cookie.
		 * @return the session cookie domain
		 */
		public String getDomain() {
			return this.domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		/**
		 * Return the path of the session cookie.
		 * @return the session cookie path
		 */
		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * Return the maximum age of the session cookie.
		 * @return the maximum age of the session cookie
		 */
		public Duration getMaxAge() {
			return this.maxAge;
		}

		public void setMaxAge(Duration maxAge) {
			this.maxAge = maxAge;
		}

		/**
		 * Return whether to use "HttpOnly" cookies for session cookies.
		 * @return {@code true} to use "HttpOnly" cookies for session cookies.
		 */
		public Boolean getHttpOnly() {
			return this.httpOnly;
		}

		public void setHttpOnly(Boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		/**
		 * Return whether to always mark the session cookie as secure.
		 * @return {@code true} to mark the session cookie as secure even if the request
		 * that initiated the corresponding session is using plain HTTP
		 */
		public Boolean getSecure() {
			return this.secure;
		}

		public void setSecure(Boolean secure) {
			this.secure = secure;
		}

		public SameSite getSameSite() {
			return this.sameSite;
		}

		public void setSameSite(SameSite sameSite) {
			this.sameSite = sameSite;
		}

	}

	public enum SameSite {

		/**
		 * Cookies are sent in both first-party and cross-origin requests.
		 */
		NONE("None"),

		/**
		 * Cookies are sent in a first-party context, also when following a link to the
		 * origin site.
		 */
		LAX("Lax"),

		/**
		 * Cookies are only sent in a first-party context (i.e. not when following a link
		 * to the origin site).
		 */
		STRICT("Strict");

		private final String attribute;

		SameSite(String attribute) {
			this.attribute = attribute;
		}

		public String attribute() {
			return this.attribute;
		}

	}

}
