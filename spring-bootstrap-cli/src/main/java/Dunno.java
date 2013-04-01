import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import org.codehaus.groovy.control.CompilerConfiguration;

public class Dunno {

	public static void main(final String[] args) {
		try {
			System.setProperty("groovy.grape.report.downloads", "true");
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			CompilerConfiguration config = new CompilerConfiguration();
			config.setTargetDirectory("target/groovy");
			final BootstrapGroovyClassLoader groovyClassLoader = new BootstrapGroovyClassLoader(loader, config);
			Class parseClass = groovyClassLoader.parseClass(new File("src/main/resources/Example.groovy"));

//			GroovyScriptEngine engine = new GroovyScriptEngine(
//					new URL[] {new File("src/main/resources").toURL()});
//			Class loadScriptByName = engine.loadScriptByName("example.groovy");
//			loadScriptByName.newInstance();
//			System.out.println(loadScriptByName.getName());

			Thread thread = new Thread() {
				public void run() {
					Class<?> application;
					try {
						application = getContextClassLoader().loadClass("org.springframework.bootstrap.SpringApplication");
						Method method = application.getMethod("runComponents", Class[].class, String[].class);
						Thread.sleep(1000);
						method.invoke(null, groovyClassLoader.getLoadedClasses(), args);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				};
			};
			thread.setContextClassLoader(parseClass.getClassLoader());
			thread.start();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
