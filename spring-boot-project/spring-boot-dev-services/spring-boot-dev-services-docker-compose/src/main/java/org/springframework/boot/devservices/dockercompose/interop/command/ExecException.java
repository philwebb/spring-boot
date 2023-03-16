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

/**
 * Is thrown if process execution failed.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class ExecException extends RuntimeException {

	private final String[] command;

	private final int exitCode;

	private final String stdOut;

	private final String strErr;

	ExecException(String[] command, int exitCode, String stdOut, String strErr) {
		this(command, exitCode, stdOut, strErr, null);
	}

	ExecException(String[] command, int exitCode, String stdOut, String strErr, Throwable cause) {
		super("'%s' failed with exit code %d. Stdout: '%s', Stderr: '%s'".formatted(String.join(" ", command), exitCode,
				stdOut, strErr), cause);
		this.command = command;
		this.exitCode = exitCode;
		this.stdOut = stdOut;
		this.strErr = strErr;
	}

	String[] getCommand() {
		return this.command;
	}

	int getExitCode() {
		return this.exitCode;
	}

	String getStdOut() {
		return this.stdOut;
	}

	String getStrErr() {
		return this.strErr;
	}

}
