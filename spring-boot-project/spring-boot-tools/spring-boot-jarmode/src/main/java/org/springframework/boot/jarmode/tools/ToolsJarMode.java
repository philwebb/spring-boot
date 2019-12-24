/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Main entry point for the {@code "tools"} jar mode.
 *
 * @author Phillip Webb
 */
final class ToolsJarMode {

	static Context contextOverride;

	private final List<Command> commands;

	private final HelpCommand help;

	ToolsJarMode() {
		Context context = (contextOverride != null) ? contextOverride : new Context();
		this.commands = getCommands(context);
		this.help = new HelpCommand(context, this.commands);
	}

	private void run(String[] args) {
		run(new ArrayDeque<>(Arrays.asList(args)));
	}

	private void run(Deque<String> args) {
		if (!args.isEmpty()) {
			Command command = Command.find(this.commands, args.removeFirst());
			if (command != null) {
				command.run(args);
				return;
			}
		}
		this.help.run(args);
	}

	static List<Command> getCommands(Context context) {
		List<Command> commands = new ArrayList<Command>();
		commands.add(new DockerfileCommand(context));
		commands.add(new ListLayersCommand(context));
		commands.add(new ExtractLayersCommand(context));
		return Collections.unmodifiableList(commands);
	}

	static void main(String... args) {
		try {
			new ToolsJarMode().run(args);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
