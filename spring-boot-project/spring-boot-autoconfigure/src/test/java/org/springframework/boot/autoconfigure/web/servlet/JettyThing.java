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
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.junit.Test;

import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author pwebb
 */
public class JettyThing {

	@Test
	public void jettyAccessLogCanBeEnabled() {
		JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
		Map<String, String> map = new HashMap<>();
		map.put("server.jetty.accesslog.enabled", "true");
		bindProperties(map);
		this.customizer.customize(factory);
		JettyWebServer webServer = (JettyWebServer) factory.getWebServer();
		try {
			NCSARequestLog requestLog = getNCSARequestLog(webServer);
			assertThat(requestLog.getFilename()).isNull();
			assertThat(requestLog.isAppend()).isFalse();
			assertThat(requestLog.isExtended()).isFalse();
			assertThat(requestLog.getLogCookies()).isFalse();
			assertThat(requestLog.getLogServer()).isFalse();
			assertThat(requestLog.getLogLatency()).isFalse();
		}
		finally {
			webServer.stop();
		}
	}

	@Test
	public void jettyAccessLogCanBeCustomized() throws IOException {
		File logFile = File.createTempFile("jetty_log", ".log");
		JettyServletWebServerFactory factory = new JettyServletWebServerFactory(0);
		Map<String, String> map = new HashMap<>();
		String timezone = TimeZone.getDefault().getID();
		map.put("server.jetty.accesslog.enabled", "true");
		map.put("server.jetty.accesslog.filename", logFile.getAbsolutePath());
		map.put("server.jetty.accesslog.file-date-format", "yyyy-MM-dd");
		map.put("server.jetty.accesslog.retention-period", "42");
		map.put("server.jetty.accesslog.append", "true");
		map.put("server.jetty.accesslog.extended-format", "true");
		map.put("server.jetty.accesslog.date-format", "HH:mm:ss");
		map.put("server.jetty.accesslog.locale", "en_BE");
		map.put("server.jetty.accesslog.time-zone", timezone);
		map.put("server.jetty.accesslog.log-cookies", "true");
		map.put("server.jetty.accesslog.log-server", "true");
		map.put("server.jetty.accesslog.log-latency", "true");
		bindProperties(map);
		this.customizer.customize(factory);
		JettyWebServer webServer = (JettyWebServer) factory.getWebServer();
		NCSARequestLog requestLog = getNCSARequestLog(webServer);
		try {
			assertThat(requestLog.getFilename()).isEqualTo(logFile.getAbsolutePath());
			assertThat(requestLog.getFilenameDateFormat()).isEqualTo("yyyy-MM-dd");
			assertThat(requestLog.getRetainDays()).isEqualTo(42);
			assertThat(requestLog.isAppend()).isTrue();
			assertThat(requestLog.isExtended()).isTrue();
			assertThat(requestLog.getLogDateFormat()).isEqualTo("HH:mm:ss");
			assertThat(requestLog.getLogLocale()).isEqualTo(new Locale("en", "BE"));
			assertThat(requestLog.getLogTimeZone()).isEqualTo(timezone);
			assertThat(requestLog.getLogCookies()).isTrue();
			assertThat(requestLog.getLogServer()).isTrue();
			assertThat(requestLog.getLogLatency()).isTrue();
		}
		finally {
			webServer.stop();
		}
	}

	private NCSARequestLog getNCSARequestLog(JettyWebServer webServer) {
		RequestLog requestLog = webServer.getServer().getRequestLog();
		assertThat(requestLog).isInstanceOf(NCSARequestLog.class);
		return (NCSARequestLog) requestLog;
	}

	@Test
	public void defaultUseForwardHeadersJetty() {
		JettyServletWebServerFactory factory = spy(new JettyServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeadersJetty() {
		this.properties.setUseForwardHeaders(true);
		JettyServletWebServerFactory factory = spy(new JettyServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

	@Test
	public void deduceUseForwardHeadersJetty() {
		this.customizer.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		JettyServletWebServerFactory factory = spy(new JettyServletWebServerFactory());
		this.customizer.customize(factory);
		verify(factory).setUseForwardHeaders(true);
	}

}
