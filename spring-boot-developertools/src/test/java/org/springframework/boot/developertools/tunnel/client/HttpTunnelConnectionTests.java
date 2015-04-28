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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.developertools.tunnel.client.HttpTunnelConnection.TunnelChannel;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
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

	private String url;

	private ByteArrayOutputStream incommingData;

	private WritableByteChannel incomingChannel;

	@Mock
	private Closeable closeable;

	private MockClientHttpRequestFactory requestFactory = new MockClientHttpRequestFactory();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.url = "http://localhost:" + this.port;
		this.incommingData = new ByteArrayOutputStream();
		this.incomingChannel = Channels.newChannel(this.incommingData);
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
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		assertThat(channel.isOpen(), equalTo(true));
		channel.close();
		assertThat(channel.isOpen(), equalTo(false));
	}

	@Test
	public void closeTunnelCallsCloseableOnce() throws Exception {
		this.requestFactory.willRespondAfterDelay(1000, HttpStatus.GONE);
		WritableByteChannel channel = openTunnel(false);
		verify(this.closeable, never()).close();
		channel.close();
		channel.close();
		verify(this.closeable, times(1)).close();
	}

	@Test
	public void typicalTraffic() throws Exception {
		this.requestFactory.willRespond("hi", "=2", "=3");
		TunnelChannel channel = openTunnel(true);
		write(channel, "hello");
		write(channel, "1+1");
		write(channel, "1+2");
		assertThat(this.incommingData.toString(), equalTo("hi=2=3"));
	}

	@Test
	public void trafficWithLongPollTimeouts() throws Exception {
		for (int i = 0; i < 10; i++) {
			this.requestFactory.willRespond(HttpStatus.NO_CONTENT);
		}
		this.requestFactory.willRespond("hi");
		TunnelChannel channel = openTunnel(true);
		write(channel, "hello");
		assertThat(this.incommingData.toString(), equalTo("hi"));
		assertThat(this.requestFactory.getExecutedRequests().size(), greaterThan(10));
	}

	private void write(TunnelChannel channel, String string) throws IOException {
		channel.write(ByteBuffer.wrap(string.getBytes()));
	}

	private TunnelChannel openTunnel(boolean singleThreaded) throws Exception {
		HttpTunnelConnection connection = new HttpTunnelConnection(this.url,
				this.requestFactory,
				(singleThreaded ? new CurrentThreadExecutor() : null));
		return connection.open(this.incomingChannel, this.closeable);
	}

	private static class CurrentThreadExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			command.run();
		}

	}

	private class MockClientHttpRequestFactory implements ClientHttpRequestFactory {

		private AtomicLong seq = new AtomicLong();

		private Deque<Response> responses = new ArrayDeque<Response>();

		private List<MockRequest> executedRequests = new ArrayList<MockRequest>();

		@Override
		public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod)
				throws IOException {
			return new MockRequest(uri, httpMethod);
		}

		public void willRespond(HttpStatus... response) {
			for (HttpStatus status : response) {
				this.responses.add(new Response(0, null, status));
			}
		}

		public void willRespond(String... response) {
			for (String payload : response) {
				this.responses.add(new Response(0, payload.getBytes(), HttpStatus.OK));
			}
		}

		public void willRespondAfterDelay(int delay, HttpStatus status) {
			this.responses.add(new Response(delay, null, status));
		}

		public List<MockRequest> getExecutedRequests() {
			return this.executedRequests;
		}

		private class MockRequest extends MockClientHttpRequest {

			public MockRequest(URI uri, HttpMethod httpMethod) {
				super(httpMethod, uri);
			}

			@Override
			protected ClientHttpResponse executeInternal() throws IOException {
				MockClientHttpRequestFactory.this.executedRequests.add(this);
				Response response = MockClientHttpRequestFactory.this.responses
						.pollFirst();
				if (response == null) {
					response = new Response(0, null, HttpStatus.GONE);
				}
				return response.asHttpResponse(MockClientHttpRequestFactory.this.seq);
			}

		}

	}

	private static class Response {

		private final int delay;

		private final byte[] payload;

		private final HttpStatus status;

		public Response(int delay, byte[] payload, HttpStatus status) {
			this.delay = delay;
			this.payload = payload;
			this.status = status;
		}

		public ClientHttpResponse asHttpResponse(AtomicLong seq) {
			MockClientHttpResponse httpResponse = new MockClientHttpResponse(
					this.payload, this.status);
			waitForDelay();
			if (this.payload != null) {
				httpResponse.getHeaders().setContentLength(this.payload.length);
				httpResponse.getHeaders().setContentType(
						MediaType.APPLICATION_OCTET_STREAM);
				httpResponse.getHeaders().add("x-seq",
						Long.toString(seq.incrementAndGet()));
			}
			return httpResponse;
		}

		private void waitForDelay() {
			if (this.delay > 0) {
				try {
					Thread.sleep(this.delay);
				}
				catch (InterruptedException e) {
				}
			}
		}

	}

}
