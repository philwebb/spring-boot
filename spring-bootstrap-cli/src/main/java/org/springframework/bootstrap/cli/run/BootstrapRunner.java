
package org.springframework.bootstrap.cli.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;

public class BootstrapRunner {

	// FIXME logging

	private File file;

	private BootstrapRunnerLogLevel logLevel;

	public BootstrapRunner(File file) {
		this.file = file;
	}

	public void setWatchFile(boolean b) {
		// FIXME
	}

	public void setGuessImports(boolean b) {
		// FIXME
	}

	public void setLogLevel(BootstrapRunnerLogLevel logLevel) {
		this.logLevel = logLevel;
	}

	public void run(final String...args) throws Exception {
		if(logLevel == BootstrapRunnerLogLevel.VERBOSE) {
			System.setProperty("groovy.grape.report.downloads", "true");
		}
		final BootstrapGroovyClassLoader loader = new BootstrapGroovyClassLoader(
				getClass().getClassLoader(), getCompilerConfiguration());
		Class<?>[] classes = parse(loader, this.file);
		if(classes.length == 0) {
			throw new RuntimeException("No classes found in '" + this.file + "'");
		}
		Thread thread = new Thread(new ApplicationRunnable(classes, args));
		thread.setContextClassLoader(classes[0].getClassLoader());
		thread.start();
		thread.join();
	}


	private CompilerConfiguration getCompilerConfiguration() {
		return new CompilerConfiguration();
	}

	private Class<?>[] parse(BootstrapGroovyClassLoader loader, File file) throws CompilationFailedException, IOException {
		List<Class> classes = new ArrayList<Class>();
		Class mainClass = loader.parseClass(file);
		classes.addAll(Arrays.asList(loader.getLoadedClasses()));
		classes.remove(mainClass);
		classes.add(0, mainClass);
		return classes.toArray(new Class<?>[classes.size()]);
	}

	private static class ApplicationRunnable implements Runnable {

		private Class<?>[] classes;

		private String[] args;

		public ApplicationRunnable(Class<?>[] classes, String[] args) {
			this.classes = classes;
			this.args = args;
		}

		@Override
		public void run() {
			try {
				// FIXME we might want allow them to specify a main
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				Class<?> application = classLoader.loadClass("org.springframework.bootstrap.SpringApplication");
				Method method = application.getMethod("runComponents", Class[].class, String[].class);
				method.invoke(null, classes, args);
			}
			catch (Exception e) {
				e.printStackTrace();
				// FIXME if we are not watching we should probably exit
			}

		}

	}

}
