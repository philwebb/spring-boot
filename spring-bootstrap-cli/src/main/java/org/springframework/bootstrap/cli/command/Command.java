package org.springframework.bootstrap.cli.command;

import java.io.PrintStream;


public interface Command {

	String getName();

	boolean isOptionCommand();

	String getDescription();

	String getUsageHelp();

	void printHelp(PrintStream out) throws Exception;

	void run(String... args) throws Exception;

}
