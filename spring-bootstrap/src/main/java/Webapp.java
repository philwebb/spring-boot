import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;

public class Webapp {

	public static void main(String[] args) throws Exception {

		WebAppContext webapp = new WebAppContext();
		webapp.setContextPath("/");
		webapp.setWar("/Work/spring-bootstrap/build/libs/spring-bootstrap.war");
		webapp.setConfigurations(new Configuration[] {
			new WebInfConfiguration(),
			new AnnotationConfiguration()
		});

		Server server = new Server(8080);
		server.setHandler(webapp);
		server.start();
		server.join();
	}

}