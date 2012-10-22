import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class Webapp {

	public static void main(String[] args) throws Exception {

		AnnotationConfigWebApplicationContext ctx =
				new AnnotationConfigWebApplicationContext();
		ctx.register(WebConfig.class);

		ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(ctx));
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		context.addServlet(servletHolder, "/*");

		Server server = new Server(8080);
		server.setHandler(context);
		server.start();
		server.join();
	}

}