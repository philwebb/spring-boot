package org.springframework.bootstrap.cli;


public class NoSuchOptionException extends RuntimeException implements ShowUsageException {

	private static final long serialVersionUID = 1L;

	public NoSuchOptionException(String name) {
		super("Unknown option: --" + name);
	}

}
