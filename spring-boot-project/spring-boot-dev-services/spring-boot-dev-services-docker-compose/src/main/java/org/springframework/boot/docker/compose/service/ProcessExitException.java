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

/**
 * Is thrown if process execution failed.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ProcessExitException extends RuntimeException {

	private final int exitCode;

	private final String[] command;

	private final String stdOut;

	private final String strErr;

	ProcessExitException(int exitCode, String[] command, String stdOut, String strErr) {
		this(exitCode, command, stdOut, strErr, null);
	}

	ProcessExitException(int exitCode, String[] command, String stdOut, String strErr, Throwable cause) {
		super(buildMessage(exitCode, command, stdOut, strErr), cause);
		this.exitCode = exitCode;
		this.command = command;
		this.stdOut = stdOut;
		this.strErr = strErr;
	}

	private static String buildMessage(int exitCode, String[] command, String stdOut, String strErr) {
		return "'%s' failed with exit code %d.\n\n" + "Stdout: '%s'\n\n"
				+ "Stderr: '%s'".formatted(String.join(" ", command), exitCode, stdOut, strErr);
	}

	int getExitCode() {
		return this.exitCode;
	}

	String[] getCommand() {
		return this.command;
	}

	String getStdOut() {
		return this.stdOut;
	}

	String getStrErr() {
		return this.strErr;
	}

}
