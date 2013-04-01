package org.springframework.bootstrap.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.bootstrap.cli.command.AbstractCommand;
import org.springframework.bootstrap.cli.command.Command;
import org.springframework.bootstrap.cli.command.CreateCommand;
import org.springframework.bootstrap.cli.command.RunCommand;
import org.springframework.bootstrap.cli.command.VersionCommand;

/**
 * Spring Bootstrap Command Line Interface.
 *
 * @author Phillip Webb
 */
public class SpringBootstrapCli {

	public static final String CLI_APP = "spr";

	private List<Command> commands;

	public SpringBootstrapCli() {
		setCommands(Arrays.asList(
				new VersionCommand(),
				new RunCommand(),
				new CreateCommand()
			));
	}

	protected void setCommands(List<? extends Command> commands) {
		this.commands = new ArrayList<Command>(commands);
		this.commands.add(0, new HelpCommand());
	}

	public int runAndHandleErrors(String... args) {
		String[] argsWithoutDebugFlags = removeDebugFlags(args);
		boolean debug = argsWithoutDebugFlags.length != args.length;
		try {
			run(args);
			return 0;
		} catch (NoArgumentsException ex) {
			showUsage();
			return 1;
		} catch (Exception ex) {
			errorMessage(ex.getMessage());
			if(ex instanceof ShowUsageException) {
				showUsage();
			}
			if(debug) {
				printStackTrace(ex);
			}
			return 1;
		}
	}

	protected void run(String... args) throws Exception {
		if(args.length == 0) {
			throw new NoArgumentsException();
		}
		String commandName = args[0];
		String[] commandArguments = Arrays.copyOfRange(args, 1, args.length);
		find(commandName).run(commandArguments);
	}

	private Command find(String name) {
		boolean isOption = name.startsWith("--");
		if(isOption) {
			name = name.substring(2);
		}
		for (Command candidate : this.commands) {
			if((isOption && candidate.isOptionCommand() || !isOption) && candidate.getName().equals(name)) {
				return candidate;
			}
		}
		throw (isOption ? new NoSuchOptionException(name) : new NoSuchCommandException(name));
	}

	protected void showUsage() {
		System.out.print("usage: " + CLI_APP + " ");
		for (Command command : this.commands) {
			if(command.isOptionCommand()) {
				System.out.print("[--"+command.getName()+"] ");
			}
		}
		System.out.println("");
		System.out.println("       <command> [<args>]");
		System.out.println("");
		System.out.println("Available commands are:");
		for (Command command : this.commands) {
			if(!command.isOptionCommand()) {
				System.out.println(String.format("   %1$-15s %2$s", command.getName(),
						command.getDescription()));
			}
		}
		System.out.println("");
		System.out.println("See 'spr help <command>' for more information on a specific command.");
	}

	protected void errorMessage(String message) {
		System.err.println(message == null ? "Unexpected error" : message);
	}

	protected void printStackTrace(Exception ex) {
		System.err.println("");
		ex.printStackTrace(System.err);
		System.err.println("");
	}

	private String[] removeDebugFlags(String[] args) {
		List<String> rtn = new ArrayList<String>(args.length);
		for (String arg : args) {
			if(!("-d".equals(arg) || "--debug".equals(arg))) {
				rtn.add(arg);
			}
		}
		return rtn.toArray(new String[rtn.size()]);
	}

	private class HelpCommand extends AbstractCommand {

		public HelpCommand() {
			super("help", "Show command help", true);
		}

		@Override
		public void run(String... args) throws Exception {
			if(args.length == 0) {
				throw new NoHelpCommandArgumentsException();
			}
			String commandName = args[0];
			for (Command command : SpringBootstrapCli.this.commands) {
				if(!command.isOptionCommand() && command.getName().equals(commandName)) {
					System.out.println(CLI_APP + " " + command.getName() + " - " + command.getDescription());
					System.out.println();
					if(command.getUsageHelp() != null) {
						System.out.println("usage: " + CLI_APP + " " + command.getName() + " " + command.getUsageHelp());
						System.out.println();
					}
					command.printHelp(System.out);
					return;
				}
			}
			throw new NoSuchCommandException(commandName);
		}

	}

	public static void main(String... args) {
		System.exit(new SpringBootstrapCli().runAndHandleErrors(args));
	}
}
