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

package org.springframework.boot.developertools.livereload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * A <a href="http://livereload.com">livereload</a> server.
 *
 * @author Phillip Webb
 * @see <a href="http://livereload.com">livereload.com</a>
 * @since 1.3.0
 */
public class LiveReloadServer {

	private static Log logger = LogFactory.getLog(LiveReloadServer.class);

	private static final int DEFAULT_PORT = 35729;

	private static final int READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(4);

	private final int port;

	private ServerSocket serverSocket;

	private Thread listenThread;

	private ExecutorService executor = Executors
			.newCachedThreadPool(new WorkerThreadFactory());

	private List<Connection> connections = new ArrayList<Connection>();

	/**
	 * Create a new {@link LiveReloadServer} listening on the default port.
	 */
	public LiveReloadServer() {
		this(DEFAULT_PORT);
	}

	/**
	 * Create a new {@link LiveReloadServer} listening on the specified port.
	 * @param port the listen port
	 */
	public LiveReloadServer(int port) {
		this.port = port;
	}

	/**
	 * Start the livereload server and accept incoming connections.
	 * @throws IOException
	 */
	public synchronized void start() throws IOException {
		Assert.state(this.listenThread == null, "Server already started");
		logger.debug("Starting live reload server on port " + this.port);
		this.serverSocket = new ServerSocket(this.port);
		this.listenThread = new Thread() {
			@Override
			public void run() {
				acceptConnections();
			};
		};
		this.listenThread.setDaemon(true);
		this.listenThread.setName("Live Reload Server");
		this.listenThread.start();
	}

	private void acceptConnections() {
		do {
			try {
				Socket socket = this.serverSocket.accept();
				socket.setSoTimeout(READ_TIMEOUT);
				this.executor.execute(new ConnectionHandler(socket));
			}
			catch (SocketTimeoutException ex) {
				// Ignore
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("LiveReload server error", ex);
				}
			}
		}
		while (!this.serverSocket.isClosed());
	}

	/**
	 * Gracefully stop the livereload server.
	 * @throws IOException
	 */
	public synchronized void stop() throws IOException {
		if (true) {
			return;
		}
		if (this.listenThread != null) {
			for (Connection connection : this.connections) {
				connection.close();
			}
			this.connections.clear();
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(2, TimeUnit.MINUTES);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.serverSocket.close();
			try {
				this.listenThread.join();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			this.listenThread = null;
			this.serverSocket = null;
		}
	}

	public synchronized void triggerReload() {
		for (Connection connection : this.connections) {
			try {
				connection.triggerReload();
			}
			catch (Exception ex) {
				logger.debug("Unable to send reload message", ex);
			}
		}
	}

	private synchronized void addConnection(Connection connection) {
		this.connections.add(connection);
	}

	private synchronized void removeConnection(Connection connection) {
		this.connections.remove(connection);
	}

	private class ConnectionHandler implements Runnable {

		private final Socket socket;

		private final InputStream inputStream;

		public ConnectionHandler(Socket socket) throws IOException {
			this.socket = socket;
			this.inputStream = socket.getInputStream();
		}

		@Override
		public void run() {
			try {
				handle();
			}
			catch (ConnectionClosedException ex) {
				logger.debug("LiveReload connection closed");
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("LiveReload error", ex);
				}
			}
		}

		private void handle() throws Exception {
			try {
				try {
					OutputStream outputStream = this.socket.getOutputStream();
					try {
						Connection connection = new Connection(this.socket,
								this.inputStream, outputStream);
						runConnection(connection);
					}
					finally {
						outputStream.close();
					}
				}
				finally {
					this.inputStream.close();
				}
			}
			finally {
				this.socket.close();
			}
		}

		private void runConnection(Connection connection) throws IOException, Exception {
			try {
				addConnection(connection);
				connection.run();
			}
			finally {
				removeConnection(connection);
			}
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
