package org.springframework.bootstrap.cli.command;

import java.io.PrintStream;


public abstract class AbstractCommand implements Command {

	private String name;

	private boolean optionCommand;

	private String description;

	public AbstractCommand(String name, String description) {
		this(name, description, false);
	}

	public AbstractCommand(String name, String description, boolean optionCommand) {
		this.name = name;
		this.description = description;
		this.optionCommand = optionCommand;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public boolean isOptionCommand() {
		return this.optionCommand;
	}

	@Override
	public String getUsageHelp() {
		return null;
	}

	@Override
	public void printHelp(PrintStream out) throws Exception {
	}

}
