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

package org.springframework.boot.developertools.tunnel.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.WritableByteChannel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpTunnelConnection}.
 *
 * @author Phillip Webb
 */
public class HttpTunnelConnectionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private int port = SocketUtils.findAvailableTcpPort();

	private WritableByteChannel incomingChannel;

	@Mock
	private Closeable closeable;

	private HttpTunnelConnection connection;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		String url = "http://localhost:" + this.port;
		this.connection = new HttpTunnelConnection(url,
				new MockClientHttpRequestFactory());

	}

	@Test
	public void urlMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be null");
		new HttpTunnelConnection(null);
	}

	@Test
	public void urlMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new HttpTunnelConnection("");
	}

	@Test
	public void urlMustNotBeMalformed() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Malformed URL 'htttttp:///ttest'");
		new HttpTunnelConnection("htttttp:///ttest");
	}

	@Test
	public void closeTunnelChangesIsOpen() throws Exception {
		WritableByteChannel channel = openTunnel();
		assertThat(channel.isOpen(), equalTo(true));
		channel.close();
		assertThat(channel.isOpen(), equalTo(false));
	}

	@Test
	public void closeTunnelCallsCloseableOnce() throws Exception {
		WritableByteChannel channel = openTunnel();
		verify(this.closeable, never()).close();
		channel.close();
		channel.close();
		verify(this.closeable, times(1)).close();
	}

	@Test
	public void typicalTraffic() throws Exception {

	}

	private WritableByteChannel openTunnel() throws Exception {
		return this.connection.open(this.incomingChannel, this.closeable);
	}

	private static class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
				throws IOException {
			return null;
		}

	}

}
