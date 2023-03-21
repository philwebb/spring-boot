/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;

/**
 * Runs a process and captures the result.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProcessRunner {

	private static final Log logger = LogFactory.getLog(ProcessRunner.class);

	private final File directory;

	/**
	 * Create a new {@link ProcessRunner} instance.
	 * @param directory the working directory for the process
	 */
	ProcessRunner(Path directory) {
		this.directory = directory.toFile();
	}

	/**
	 * Runs the given {@code command}.
	 * @param command the command to run
	 * @return the output of the command
	 */
	String run(Collection<String> command) {
		return run(command.toArray(new String[0]));
	}

	/**
	 * Runs the given {@code command}. If the process exits with an error code other than
	 * zero, an {@link ProcessExitException} will be thrown.
	 * @param command the command to run
	 * @return the output of the command
	 * @throws ProcessExitException if execution failed
	 */
	String run(String... command) {
		logger.trace(LogMessage.of(() -> "Running '%s'".formatted(String.join(" ", command))));
		Process process = startProcess(command);
		ReaderThread stdOutReader = new ReaderThread(process.getInputStream(), "stdout");
		ReaderThread stdErrReader = new ReaderThread(process.getErrorStream(), "stderr");
		logger.trace("Waiting for process exit");
		int exitCode = waitForProcess(process);
		logger.trace(LogMessage.format("Process exited with exit code %d", exitCode));
		String stdOut = stdOutReader.toString();
		String stdIn = stdErrReader.toString();
		if (exitCode != 0) {
			throw new ProcessExitException(exitCode, command, stdOut, stdIn);
		}
		return stdOut;
	}

	private Process startProcess(String[] command) {
		try {
			return new ProcessBuilder(command).directory(this.directory).start();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to start command %s".formatted(String.join(" ", command)));
		}
	}

	private int waitForProcess(Process process) {
		try {
			return process.waitFor();
		}
		catch (InterruptedException ex) {
			throw new IllegalStateException("Interrupted waiting for %s".formatted(process));
		}
	}

	/**
	 * Thread used to read stream input from the process.
	 */
	private static class ReaderThread extends Thread {

		private final InputStream source;

		private final ByteArrayOutputStream content = new ByteArrayOutputStream();

		private final CountDownLatch latch = new CountDownLatch(1);

		ReaderThread(InputStream source, String name) {
			this.source = source;
			setName("OutputReader-" + name);
			setDaemon(true);
			start();
		}

		@Override
		public void run() {
			try {
				this.source.transferTo(this.content);
				this.latch.countDown();
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to read process stream", ex);
			}
		}

		@Override
		public String toString() {
			try {
				this.latch.await();
				return new String(this.content.toByteArray(), StandardCharsets.UTF_8);
			}
			catch (InterruptedException e) {
				return null;
			}
		}

	}

}
