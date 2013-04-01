package org.springframework.bootstrap.cli.command;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import static java.util.Arrays.*;


public class CreateCommand extends OptionParsingCommand {

	public CreateCommand() {
		super("create", "Create an new spring groovy script");
	}

	@Override
	public String getUsageHelp() {
		return "[options] <file>";
	}

	@Override
	protected OptionParser createOptionParser() {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(asList("overwite", "f"), "Overwrite any existing file");
		parser.accepts("type", "Create a specific application type").withOptionalArg().ofType(String.class).describedAs("web, batch, integration");
		return parser;
	}

	@Override
	protected void run(OptionSet options) {
		throw new IllegalStateException("Not implemented"); // FIXME
	}

}
