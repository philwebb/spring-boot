package org.springframework.bootstrap.cli.command;

import java.awt.Desktop;
import java.io.File;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.codehaus.groovy.control.CompilationFailedException;
import org.springframework.bootstrap.cli.run.BootstrapRunner;
import org.springframework.bootstrap.cli.run.BootstrapRunnerLogLevel;

import static java.util.Arrays.*;

public class RunCommand extends OptionParsingCommand {

	private OptionSpec noWatchOption;

	private OptionSpec editOption;

	private OptionSpec noGuessOption;

	private OptionSpec verboseOption;

	private OptionSpec quiteOption;

	public RunCommand() {
		super("run", "Run a spring groovy script");
	}

	@Override
	public String getUsageHelp() {
		return "[options] <file>";
	}

	@Override
	protected OptionParser createOptionParser() {
		OptionParser parser = new OptionParser();
		this.noWatchOption = parser.accepts("no-watch", "Do not watch the specified file for changes");
		this.editOption = parser.acceptsAll(asList("edit", "e"), "Open the file with the default system editor");
		this.noGuessOption = parser.accepts("no-guess", "Do not attempt to guess imports");
		this.verboseOption = parser.acceptsAll(asList("verbose", "v"), "Verbose logging");
		this.quiteOption = parser.acceptsAll(asList("quiet", "q"), "Quiet logging");
		return parser;
	}

	@Override
	protected void run(OptionSet options) throws Exception {
		List<String> nonOptionArguments = options.nonOptionArguments();
		if(nonOptionArguments.size() == 0) {
			throw new RuntimeException("Please specify a file to run");
		}
		String filename = nonOptionArguments.get(0);
		List<String> args = nonOptionArguments.subList(1, nonOptionArguments.size());
		File file = new File(filename);
		if(!file.isFile() || !file.canRead()) {
			throw new RuntimeException("Unable to read '" + filename + "'");
		}
		if(options.has(this.editOption)) {
			Desktop.getDesktop().edit(file);
		}

		BootstrapRunner runner = newBootstrapRunner(file);
		runner.setWatchFile(!options.has(this.noWatchOption));
		runner.setGuessImports(!options.has(this.noGuessOption));
		if (options.has(this.verboseOption)) {
			runner.setLogLevel(BootstrapRunnerLogLevel.VERBOSE);
		}
		else if (options.has(this.quiteOption)) {
			runner.setLogLevel(BootstrapRunnerLogLevel.QUIET);
		}
		try {
			runner.run(args.toArray(new String[args.size()]));
		}
		catch (CompilationFailedException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	protected BootstrapRunner newBootstrapRunner(File file) {
		return new BootstrapRunner(file);
	}

}
