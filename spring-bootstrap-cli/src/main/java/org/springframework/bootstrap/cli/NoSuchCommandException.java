package org.springframework.bootstrap.cli;


public class NoSuchCommandException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoSuchCommandException(String name) {
		super(String.format("%1$s: '%2$s' is not a valid command. See '%1$s --help'.",
				SpringBootstrapCli.CLI_APP, name));
	}

}
