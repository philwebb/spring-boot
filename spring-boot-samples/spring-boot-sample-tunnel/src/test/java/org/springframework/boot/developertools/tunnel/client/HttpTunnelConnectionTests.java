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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.developertools.tunnel.client.HttpTunnelConnection.TunnelChannel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
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
@Ignore
public class HttpTunnelConnectionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private int port = SocketUtils.findAvailableTcpPort();

	private ByteArrayOutputStream incommingData;

	private WritableByteChannel incomingChannel;

	@Mock
	private Closeable closeable;

	private MockClientHttpRequestFactory requestFactory = new MockClientHttpRequestFactory();

	private HttpTunnelConnection connection;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		String url = "http://localhost:" + this.port;
		this.incommingData = new ByteArrayOutputStream();
		this.incomingChannel = Channels.newChannel(this.incommingData);
		this.connection = new HttpTunnelConnection(url, this.requestFactory);
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
		WritableByteChannel channel = openTunnel();
		Thread.sleep(100);
		channel.write(ByteBuffer.wrap("hello".getBytes()));
		Thread.sleep(100);
		this.requestFactory.send("hi");
		Thread.sleep(100);
		channel.write(ByteBuffer.wrap("1+1".getBytes()));
		Thread.sleep(100);
		this.requestFactory.send("=2");
		Thread.sleep(100);
		this.requestFactory.send(HttpStatus.GONE);
		Thread.sleep(100);
		Thread.sleep(1000000L);
		List<MockClientHttpRequest> requests = this.requestFactory.getRequests();
		for (MockClientHttpRequest mockClientHttpRequest : requests) {
			System.out.println(mockClientHttpRequest);
		}
		assertThat(requests.get(0).getBodyAsString(), equalTo("hello"));
		assertThat(requests.get(1).getBodyAsString(), equalTo("1+1"));
	}

	private TunnelChannel openTunnel() throws Exception {
		return this.connection.open(this.incomingChannel, this.closeable);
	}

	private class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

		private AtomicLong seq = new AtomicLong();

		private final BlockingDeque<ClientHttpResponse> responses = new LinkedBlockingDeque<ClientHttpResponse>();

		private List<MockClientHttpRequest> requests = Collections
				.synchronizedList(new ArrayList<MockClientHttpRequest>());

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
				throws IOException {
			MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri) {

				@Override
				protected ClientHttpResponse executeInternal() throws IOException {
					if (super.executeInternal() == null) {
						try {
							ClientHttpResponse response = MockClientHttpRequestFactory.this.responses
									.pollFirst(2, TimeUnit.DAYS);
							setResponse(response);
						}
						catch (InterruptedException ex) {
						}
					}
					return super.executeInternal();
				}

			};
			this.requests.add(request);
			return request;
		}

		public void send(String content) throws InterruptedException {
			MockClientHttpResponse response = new MockClientHttpResponse(
					content.getBytes(), HttpStatus.OK);
			response.getHeaders().add("x-seq", "" + this.seq.incrementAndGet());
			this.responses.addLast(response);
		}

		public void send(HttpStatus status) throws InterruptedException {
			this.responses.put(new MockClientHttpResponse((byte[]) null, status));
		}

		public List<MockClientHttpRequest> getRequests() {
			return this.requests;
		}

	}

}
