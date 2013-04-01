package org.springframework.bootstrap.cli.command;

import java.io.IOException;
import java.io.PrintStream;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public abstract class OptionParsingCommand extends AbstractCommand {

	private OptionParser parser;

	public OptionParsingCommand(String name, String description) {
		super(name, description);
		this.parser = createOptionParser();
	}

	protected abstract OptionParser createOptionParser();

	@Override
	public void printHelp(PrintStream out) throws IOException {
		this.parser.printHelpOn(out);
	}

	@Override
	public final void run(String... args) throws Exception {
		OptionSet options = parser.parse(args);
		run(options);
	}

	protected abstract void run(OptionSet options) throws Exception;

}
