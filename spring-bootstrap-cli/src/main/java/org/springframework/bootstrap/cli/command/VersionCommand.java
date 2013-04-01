package org.springframework.bootstrap.cli.command;


public class VersionCommand extends AbstractCommand {

	public VersionCommand() {
		super("version", "Show the version", true);
	}

	@Override
	public void run(String... args) {
		throw new IllegalStateException("Not implemented"); // FIXME
	}

}
