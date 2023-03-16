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

package org.springframework.boot.devservices.dockercompose.interop.command;

import java.io.ByteArrayOutputStream;
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
 * Executes commands using {@link Process}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class Exec {

	private static final Log logger = LogFactory.getLog(Exec.class);

	private final Path workingDirectory;

	/**
	 * Constructor.
	 * @param workingDirectory the working directory for the process
	 */
	Exec(Path workingDirectory) {
		this.workingDirectory = workingDirectory;
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
	 * zero, an {@link ExecException} will be thrown.
	 * @param command the command to run
	 * @return the output of the command
	 * @throws ExecException if execution failed
	 */
	String run(String... command) {
		if (logger.isTraceEnabled()) {
			logger.trace(LogMessage.format("Running '%s'", String.join(" ", command)));
		}
		int exitCode = 0;
		String stdOut = "";
		String stdErr = "";
		try {
			Process process = new ProcessBuilder(command).directory(this.workingDirectory.toFile()).start();
			OutputReader output = new OutputReader("stdout", process.getInputStream());
			output.start();
			OutputReader error = new OutputReader("stderr", process.getErrorStream());
			error.start();
			logger.trace("Waiting for process exit");
			exitCode = process.waitFor();
			logger.trace(LogMessage.format("Process exited with exit code %d", exitCode));
			stdOut = output.getContentAsString();
			stdErr = error.getContentAsString();
			if (exitCode != 0) {
				throw new ExecException(command, exitCode, stdOut, stdErr);
			}
			return stdOut;
		}
		catch (IOException | InterruptedException ex) {
			throw new ExecException(command, exitCode, stdOut, stdErr, ex);
		}
	}

	private static class OutputReader extends Thread {

		private final InputStream stream;

		private final ByteArrayOutputStream content = new ByteArrayOutputStream();

		private final CountDownLatch latch = new CountDownLatch(1);

		OutputReader(String name, InputStream stream) {
			this.stream = stream;
			setName("OutputReader-" + name);
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				this.stream.transferTo(this.content);
				this.latch.countDown();
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to read stream", ex);
			}
		}

		byte[] getContent() throws InterruptedException {
			this.latch.await();
			return this.content.toByteArray();
		}

		String getContentAsString() throws InterruptedException {
			return new String(getContent(), StandardCharsets.UTF_8);
		}

	}

}
