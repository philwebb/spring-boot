package org.springframework.bootstrap.cli;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.bootstrap.cli.command.Command;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link SpringBootstrapCli}.
 *
 * @author Phillip Webb
 */
public class SpringBootstrapCliTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SpringBootstrapCli cli;

	@Mock
	private Command regularCommand;

	@Mock
	private Command optionCommand;

	private Set<Call> calls = EnumSet.noneOf(Call.class);

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.cli = new SpringBootstrapCli() {

			@Override
			protected void showUsage() {
				calls.add(Call.SHOW_USAGE);
				super.showUsage();
			};

			@Override
			protected void errorMessage(String message) {
				calls.add(Call.ERROR_MESSAGE);
				super.errorMessage(message);
			}

			@Override
			protected void printStackTrace(Exception ex) {
				calls.add(Call.PRINT_STACK_TRACE);
				super.printStackTrace(ex);
			}
		};
		given(this.regularCommand.getName()).willReturn("command");
		given(this.regularCommand.getDescription()).willReturn("A regular command");
		given(this.optionCommand.getName()).willReturn("option");
		given(this.optionCommand.getDescription()).willReturn("An optional command");
		given(this.optionCommand.isOptionCommand()).willReturn(true);
		this.cli.setCommands(Arrays.asList(regularCommand, optionCommand));
	}

	@Test
	public void runWithoutArguments() throws Exception {
		thrown.expect(NoArgumentsException.class);
		cli.run();
	}

	@Test
	public void runCommand() throws Exception {
		cli.run("command", "--arg1", "arg2");
		verify(regularCommand).run("--arg1", "arg2");
	}

	@Test
	public void runOptionCommand() throws Exception {
		cli.run("--option", "--arg1", "arg2");
		verify(optionCommand).run("--arg1", "arg2");
	}

	@Test
	public void runOptionCommandWithoutOption() throws Exception {
		cli.run("option", "--arg1", "arg2");
		verify(optionCommand).run("--arg1", "arg2");
	}

	@Test
	public void runOptionOnNonOptionCommand() throws Exception {
		thrown.expect(NoSuchOptionException.class);
		cli.run("--command", "--arg1", "arg2");
	}

	@Test
	public void missingCommand() throws Exception {
		thrown.expect(NoSuchCommandException.class);
		cli.run("missing");
	}

	@Test
	public void handlesSuccess() throws Exception {
		int status = cli.runAndHandleErrors("--option");
		assertThat(status, equalTo(0));
		assertThat(this.calls, equalTo((Set) EnumSet.noneOf(Call.class)));
	}

	@Test
	public void handlesNoArgumentsException() throws Exception {
		int status = cli.runAndHandleErrors();
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set) EnumSet.of(Call.SHOW_USAGE)));
	}

	@Test
	public void handlesNoSuchOptionException() throws Exception {
		int status = cli.runAndHandleErrors("--missing");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set) EnumSet.of(Call.ERROR_MESSAGE, Call.SHOW_USAGE)));
	}

	@Test
	public void handlesRegularException() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run();
		int status = cli.runAndHandleErrors("command");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set) EnumSet.of(Call.ERROR_MESSAGE)));
	}

	@Test
	public void handlesExceptionWithDashD() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run((String[]) anyObject());
		int status = cli.runAndHandleErrors("command", "-d");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set) EnumSet.of(Call.ERROR_MESSAGE, Call.PRINT_STACK_TRACE)));
	}

	@Test
	public void handlesExceptionWithDashDashDebug() throws Exception {
		willThrow(new RuntimeException()).given(this.regularCommand).run((String[]) anyObject());
		int status = cli.runAndHandleErrors("command", "--debug");
		assertThat(status, equalTo(1));
		assertThat(this.calls, equalTo((Set) EnumSet.of(Call.ERROR_MESSAGE, Call.PRINT_STACK_TRACE)));
	}

	@Test
	public void exceptionMessages() throws Exception {
		assertThat(new NoSuchOptionException("name").getMessage(), equalTo("Unknown option: --name"));
		assertThat(new NoSuchCommandException("name").getMessage(), equalTo("spr: 'name' is not a valid command. See 'spr --help'."));
	}

	@Test
	public void help() throws Exception {
		cli.run("help", "command");
		verify(regularCommand).printHelp((PrintStream) anyObject());
	}

	@Test
	public void helpNoCommand() throws Exception {
		thrown.expect(NoHelpCommandArgumentsException.class);
		cli.run("help");
	}

	@Test
	public void helpUnknownCommand() throws Exception {
		thrown.expect(NoSuchCommandException.class);
		cli.run("help", "missing");
	}

	private static enum Call {
		SHOW_USAGE, ERROR_MESSAGE, PRINT_STACK_TRACE
	}
}
