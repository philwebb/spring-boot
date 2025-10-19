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

package org.springframework.boot.test.web.htmlunit;

import org.htmlunit.BrowserVersion;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.Capabilities;

import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver;

/**
 * HTML Unit {@link WebConnectionHtmlUnitDriver} optionally backed by a
 * {@link LocalTestWebServer}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class LocalTestWebServerWebConnectionHtmlUnitDriver extends WebConnectionHtmlUnitDriver {

	private @Nullable LocalTestWebServer localTestWebServer;

	public LocalTestWebServerWebConnectionHtmlUnitDriver(@Nullable LocalTestWebServer localTestWebServer) {
		this.localTestWebServer = localTestWebServer;
	}

	public LocalTestWebServerWebConnectionHtmlUnitDriver(@Nullable LocalTestWebServer localTestWebServer,
			boolean enableJavascript) {
		super(enableJavascript);
		this.localTestWebServer = localTestWebServer;
	}

	public LocalTestWebServerWebConnectionHtmlUnitDriver(@Nullable LocalTestWebServer localTestWebServer,
			BrowserVersion browserVersion) {
		super(browserVersion);
		this.localTestWebServer = localTestWebServer;
	}

	public LocalTestWebServerWebConnectionHtmlUnitDriver(@Nullable LocalTestWebServer localTestWebServer,
			Capabilities capabilities) {
		super(capabilities);
		this.localTestWebServer = localTestWebServer;
	}

	@Override
	public void get(String url) {
		super.get(resolve(url));
	}

	private String resolve(String url) {
		return (this.localTestWebServer != null) ? this.localTestWebServer.uriBuilder(url).toString() : url;
	}

}
