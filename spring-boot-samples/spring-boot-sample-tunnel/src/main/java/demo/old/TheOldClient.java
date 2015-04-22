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

package demo.old;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * @author pwebb
 */
public class TheOldClient {

	private static final int READ_TIMEOUT = 4000;

	private ServerSocket serverSocket;

	private ExecutorService executor = Executors
			.newCachedThreadPool(new WorkerThreadFactory());

	private void run() throws Exception {
		try {
			this.serverSocket = new ServerSocket(8000);
			Thread listenThread = new Thread() {
				@Override
				public void run() {
					serverLoop();
				};
			};
			listenThread.setDaemon(false);
			listenThread.setName("Remote Debug Server");
			listenThread.start();
		}
		catch (IOException e) {
			throw new UnsupportedOperationException("Auto-generated method stub", e);
		}
	}

	private void serverLoop() {
		do {
			try {
				final Socket socket = this.serverSocket.accept();
				socket.setSoTimeout(READ_TIMEOUT);
				final InputStream inputStream = socket.getInputStream();
				this.executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							handleConnection(socket, inputStream);
						}
						catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});
			}
			catch (SocketTimeoutException ex) {
				// Ignore
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		while (!this.serverSocket.isClosed());
	}

	private void handleConnection(Socket socket, InputStream inputStream)
			throws Exception {
		StandardWebSocketClient client = new StandardWebSocketClient();
		// String add = "ws://localhost:8080/tunnel";
		String add = "ws://192.168.1.56:8080/tunnel";
		// String add = "wss://tunnel.cfapps.io:4443/tunnel";
		WebSocketSession session = client.doHandshake(
				new TunnelWebSocketClientHandler(socket, inputStream), add).get();
	}

	/*
	 */

	public static void main(String[] args) {
		try {
			new TheOldClient().run();
			Thread.sleep(10000);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@link ThreadFactory} to create the worker threads,
	 */
	private static class WorkerThreadFactory implements ThreadFactory {

		private final AtomicInteger threadNumber = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(true);
			thread.setName("Live Reload #" + this.threadNumber.getAndIncrement());
			return thread;
		}

	}

}
