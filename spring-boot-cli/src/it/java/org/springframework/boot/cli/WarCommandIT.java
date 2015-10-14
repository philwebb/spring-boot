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

package org.springframework.boot.cli;

import java.io.File;

import org.junit.Test;
import org.springframework.boot.cli.command.archive.WarCommand;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker;
import org.springframework.boot.cli.infrastructure.CommandLineInvoker.Invocation;
import org.springframework.boot.loader.tools.JavaExecutable;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Integration test for {@link WarCommand}.
 *
 * @author Andrey Stolyarov
 */
public class WarCommandIT {

	private final CommandLineInvoker cli = new CommandLineInvoker(new File(
			"src/it/resources/war-command"));

	@Test
	public void warCreation() throws Exception {
		File war = new File("target/test-app.war");
		Invocation invocation = this.cli.invoke("war", war.getAbsolutePath(),
				"war.groovy");
		invocation.await();
		assertTrue(war.exists());
		Process process = new JavaExecutable().processBuilder("-jar",
				war.getAbsolutePath()).start();
		invocation = new Invocation(process);
		invocation.await();
		assertThat(invocation.getErrorOutput(), containsString("onStart error"));
		assertThat(invocation.getStandardOutput(), containsString("/WEB-INF/lib-provided/tomcat-embed-core"));
		process.destroy();
	}

}
