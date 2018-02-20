/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author pwebb
 */
public class UndertowThing {

	@Test
	public void skipNullElementsForUndertow() {
		UndertowServletWebServerFactory factory = mock(
				UndertowServletWebServerFactory.class);
		this.customizer.customize(factory);
		verify(factory, never()).setAccessLogEnabled(anyBoolean());
	}

	@Test
	public void defaultUseForwardHeadersUndertow() {
		UndertowServletWebServerFactory factory = spy(
				new UndertowServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeadersUndertow() {
		this.properties.setUseForwardHeaders(true);
		UndertowServletWebServerFactory factory = spy(
				new UndertowServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void deduceUseForwardHeadersUndertow() {
		this.customizer.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		UndertowServletWebServerFactory factory = spy(
				new UndertowServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void customizeUndertowAccessLog() {
		Map<String, String> map = new HashMap<>();
		map.put("server.undertow.accesslog.enabled", "true");
		map.put("server.undertow.accesslog.pattern", "foo");
		map.put("server.undertow.accesslog.prefix", "test_log");
		map.put("server.undertow.accesslog.suffix", "txt");
		map.put("server.undertow.accesslog.dir", "test-logs");
		map.put("server.undertow.accesslog.rotate", "false");
		bindProperties(map);
		UndertowServletWebServerFactory factory = spy(
				new UndertowServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setAccessLogEnabled(true);
		verify(factory).setAccessLogPattern("foo");
		verify(factory).setAccessLogPrefix("test_log");
		verify(factory).setAccessLogSuffix("txt");
		verify(factory).setAccessLogDirectory(new File("test-logs"));
		verify(factory).setAccessLogRotate(false);
	}
}
