
package org.springframework.bootstrap.shell;

import java.io.IOException;
import static java.util.Arrays.*;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class SpringBootstrapShell {

	private OptionSpec<Void> help;

	private void run(String[] args) throws IOException {
		OptionParser parser = new OptionParser();
		parser.acceptsAll(asList("edit", "e"), "Open the file for editing and watch for subsequent changes");
		parser.acceptsAll(asList("watch", "w"), "Watch the file for changes");
		this.help = parser.acceptsAll(asList("help", "?"), "show help").forHelp();

		OptionSet options = parser.parse(args);

		if (options.has(help)) {
			parser.printHelpOn(System.out);
			return;
		}

		System.out.println(options.nonOptionArguments());

	}

	public static void main(String[] args) {
		try {
			new SpringBootstrapShell().run(args);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
