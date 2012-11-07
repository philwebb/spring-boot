import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		AnnotationConfigWebApplicationContext ctx =
				new AnnotationConfigWebApplicationContext();
		ctx.register(WebConfig.class);

		Dynamic ds = servletContext.addServlet("main", new DispatcherServlet(ctx));
		ds.addMapping("/");
	}

}
