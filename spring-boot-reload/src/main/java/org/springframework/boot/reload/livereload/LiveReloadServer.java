/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.reload.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A LiveReload server.
 *
 * @author Phillip Webb
 * @see <a href="http://livereload.com">livereload.com</a>
 */
public class LiveReloadServer {

	private static final int READ_TIMEOUT = 4000;

	private ServerSocket serverSocket;

	private Thread listenThread;

	private ExecutorService executor = Executors
			.newCachedThreadPool(new WorkerThreadFactory());

	private List<Connection> connections = new ArrayList<Connection>();

	public void start() throws IOException {
		this.serverSocket = new ServerSocket(35729);
		this.listenThread = new Thread() {
			@Override
			public void run() {
				serverLoop();
			};
		};
		this.listenThread.setDaemon(true);
		this.listenThread.setName("Live Reload Server");
		this.listenThread.start();
	}

	public synchronized void reload() {
		for (Connection connection : this.connections) {
			try {
				connection.reload();
			}
			catch (Exception ex) {
			}
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
						}
					}
				});
			}
			catch (IOException ex) {
			}
		}
		while (!this.serverSocket.isClosed());
	}

	private void handleConnection(Socket socket, InputStream inputStream)
			throws Exception {
		try {
			try {
				OutputStream outputStream = socket.getOutputStream();
				try {
					runConnection(socket, inputStream, outputStream);
				}
				finally {
					outputStream.close();
				}
			}
			finally {
				inputStream.close();
			}
		}
		finally {
			socket.close();
		}
	}

	private void runConnection(Socket socket, InputStream inputStream,
			OutputStream outputStream) throws IOException, Exception {
		Connection connection = new Connection(socket, inputStream, outputStream);
		try {
			addConnection(connection);
			connection.run();
		}
		finally {
			removeConnection(connection);
		}
	}

	private synchronized void addConnection(Connection connection) {
		this.connections.add(connection);
	}

	private synchronized void removeConnection(Connection connection) {
		this.connections.remove(connection);
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

	// FIXME delete
	public static void main(String[] args) throws IOException {
		LiveReloadServer liveReloadServer = new LiveReloadServer();
		liveReloadServer.start();
		for (int i = 0; i < 10; i++) {
			try {
				Thread.sleep(1500);
			}
			catch (InterruptedException e) {
			}
			liveReloadServer.reload();
		}

	}

}
